# ActionArms コードベース全体調査レポート

調査日: 2026-03-09

## 1. プロジェクト構成

```
ActionArms/
├── common/    # 共通コード（Fabric/Forge 両対応）
├── fabric/    # Fabric 固有実装
├── forge/     # Forge 固有実装
├── config/    # Checkstyle/SpotBugs 設定
└── docs/      # ドキュメント
```

- Minecraft 1.20.1 / Java 17 / Architectury API
- glTF 3D モデル描画（jglTF ライブラリ）
- Cloth Config / ModMenu 連携

---

## 2. システム全体像

```
┌─────────────────────────────────────────────────────────────────┐
│                        SERVER SIDE                              │
│                                                                 │
│  ActionArms.init()                                              │
│    ├── Registration (アイテム/エンティティ/サウンド登録)          │
│    ├── Networking (パケット初期化)                               │
│    ├── ServerHudManager.tick() (HUD状態追跡・差分同期)           │
│    └── ItemUniqueManager.clearOld()                             │
│                                                                 │
│  MixinServerPlayerEntity                                        │
│    ├── GunController.tick() (銃操作ロジック)                     │
│    │     └── LeverActionGunComponent (状態機械)                 │
│    └── KeyInputManager (サーバー側キー入力状態)                  │
│                                                                 │
│  MixinServerWorld                                               │
│    └── EntityRecordManager (ラグ補償用エンティティ履歴)          │
│                                                                 │
│  BulletEntity (弾エンティティ)                                   │
│    ├── レイキャスト + 連続衝突検出                               │
│    ├── ラグ補償（過去状態の線形補間）                            │
│    └── ヘッドショット判定                                       │
├─────────────────────────────────────────────────────────────────┤
│                    NETWORK (Architectury)                        │
│                                                                 │
│  C2S: KeyInputPacket, AimPacket                                 │
│  S2C: ItemAnimationEventPacket, HudStatePacket, RecoilPacket    │
├─────────────────────────────────────────────────────────────────┤
│                        CLIENT SIDE                              │
│                                                                 │
│  ActionArmsClient                                               │
│    ├── AAKeys (FIRE/AIM/RELOAD/COCK キーバインド)               │
│    ├── ClientKeyInputManager (キーポーリング → C2S送信)          │
│    ├── ClientAimManager (エイム状態 + カメラ揺れ)               │
│    ├── AAHudRenderer (弾薬表示 + クロスヘア)                    │
│    ├── ClientHudManager (HUD状態キャッシュ)                     │
│    └── glTF レンダリングシステム                                │
│          ├── GltfModelManager (モデル読込)                       │
│          ├── GltfMetadataManager (JSON設定読込)                  │
│          ├── ItemAnimationManager (アニメーション時間管理)       │
│          ├── ActionArmsItemRenderer (銃描画)                    │
│          └── GltfMeshRenderer (メッシュ描画)                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. コアシステム

### 3.1 エントリーポイント

| クラス | 役割 |
|--------|------|
| `ActionArms` | mod初期化。preInit→init順で Registration, Config, Networking, ティック登録 |
| `Registration` | DeferredRegister でアイテム/エンティティ/サウンド登録 |
| `AAConfig` | Cloth Config 連携。エイムトグル、弾ダメージ設定 |

### 3.2 Mixin 一覧

| Mixin | 対象 | 注入内容 |
|-------|------|----------|
| `MixinPlayerEntity` | PlayerEntity | AimManager 付与、tick注入、スイング抑止 |
| `MixinClientPlayerEntity` | ClientPlayerEntity | HasKeyInputManager 付与 |
| `MixinServerPlayerEntity` | ServerPlayerEntity | GunController + KeyInputManager 付与、tick注入 |
| `MixinServerWorld` | ServerWorld | EntityRecordManager 付与、tick前スナップショット |
| `MixinHeldItemRenderer` | HeldItemRenderer | glTFアイテムの手位置・スイング抑止 |
| `MixinInGameHud` | InGameHud | 銃所持時クロスヘア非表示 |
| `MixinItemRenderer` | ItemRenderer | glTFアイテムをカスタムレンダラーで描画 |
| `MixinPlayerEntityRenderer` | PlayerEntityRenderer | 銃所持時にCROSSBOW_HOLDポーズ |
| `MixinItemStack` | ItemStack | 空NBTをnull化（アイテムマージ対応） |
| `DamageSourcesAccessor` | DamageSources | Registry<DamageType>アクセス |
| `GameRendererInvoker` | GameRenderer | getFov()呼び出し |
| `ServerWorldInvoker` | ServerWorld | getEntityLookup()呼び出し |

---

## 4. アイテムシステム（3層構造）

### 4.1 Item層

| クラス | 役割 |
|--------|------|
| `GunItem` | 銃基底クラス（GlftModelItem マーカー実装） |
| `LeverActionGunItem` | レバーアクション銃の完全実装。射撃・拡散角計算・コンテキスト生成 |
| `BulletItem` | 弾薬アイテム。BulletData 保持 |
| `ItemUniqueManager` | ItemStack の UUID 一意性管理（重複検出・修正） |

### 4.2 Component層（NBT永続化 + 状態機械）

| クラス | 役割 |
|--------|------|
| `IComponent` | NBT read/write 基盤。execute(変更検出)/query(読み取り専用)/update(常に書込) |
| `LeverActionGunComponent` | **有限状態機械**。射撃・サイクル・リロードの全状態遷移を管理 |
| `MagazineComponent` | チューブマガジン。LinkedList<BulletData>のLIFOキュー |

**LeverActionGunComponent 状態遷移:**
```
[待機] ──(trigger)──→ [射撃] ──(fireCoolTime)──→ [ハンマー未準備]
                                                    │
[ハンマー未準備] ──(cycle)──→ [レバー下げ中] ──→ [レバー上げ中] ──→ [ハンマー準備完了]
                                                    │
[任意] ──(reload)──→ [リロード中] ──(reloadTime=0)──→ [弾装填] ──(reloadCoolTime)──→ [完了]
```

### 4.3 Data層（イミュータブル record）

| クラス | 役割 |
|--------|------|
| `IData` | 基底インターフェース（`getId()` のみ） |
| `BulletData` | 弾パラメータ: id, damage, headshotDamage |
| `LeverActionGunData` | 銃パラメータ: 13フィールド（タイミング、キャンセル可能長、拡散角等） |
| `MagazineData` | マガジンパラメータ: id, capacity, allowBullet (Predicate) |
| `AADataRegistry` | 全データの一元管理レジストリ。NBTではID参照方式で保存 |

### 4.4 Utility インターフェース

| クラス | 役割 |
|--------|------|
| `FireTrigger` | 射撃契約。trigger() + canTrigger() |
| `CyclingLever` | レバー操作契約。cycle() + canCycle() + shouldCycle() |
| `Reloadable` | リロード契約。reload() + canReload() + shouldReload() |
| `Chamber` | 薬室。Cartridge 1発管理 |
| `Cartridge` | 薬筒。BulletData保持 + NBTシリアライズ |
| `AnimationContext` | アニメーション送信（S2C ItemAnimationEventPacket 経由） |
| `LeverActionPlaySoundContext` | サウンド再生（CYCLE/RELOAD/FIRE/DRY_FIRE） |

---

## 5. 弾エンティティ（BulletEntity）

### 衝突判定

1. **ブロック判定**: 標準 Raycast
2. **エンティティ判定**: ラグ補償付き連続衝突検出
   - EntityRecordManager から過去 4-5 tick のスナップショット取得
   - 線形補間で正確な衝突時間計算
   - `CollisionDetector`: 球-AABB 連続衝突検出（相対速度・展開ボックス方式）

### ヘッドショット判定

- エンティティの眼球位置中心にヒットボックス生成
- 横幅 ≤ 2、高さ ≤ 4 のエンティティが対象
- エンダードラゴン・身長 ≤ 1 は全身判定
- Raycast で頭部通過を確認

### ラグ補償

- `EntityRecordManager`: 環状バッファ（10フレーム = 最大500ms）
- ServerWorld tick前に全エンティティのスナップショット取得
- 弾の衝突判定時に過去状態を参照・補間

---

## 6. キー入力フロー

```
Client: AAKeys (Keybind) → ClientKeyInputManager (ポーリング)
  ↓ KeyInputPacket (C2S, 毎フレーム)
Server: KeyInputManager.input() → 環状バッファに記録
  ↓
GunController.tick()
  ├── isTurnPressWithin(FIRE) → trigger()
  ├── isTurnPressWithin(COCK) → cycle()
  └── isTurnPressWithin(RELOAD) → reload()
  ↓ killTurnPressWithin() でキー入力消費（重複防止）
```

**エイムフロー:**
```
Client: ClientAimManager → AimPacket (C2S)
Server: AimManager.setAiming()
```

---

## 7. glTF レンダリングシステム

### パイプライン

```
[.glb ファイル] → GltfModelConverter → ProcessedGltfModel (不変)
                    ├── GltfVertexExtractor (頂点)
                    ├── GltfSkinExtractor (スキン)
                    ├── GltfAnimationExtractor (アニメーション)
                    └── GltfMaterialExtractor (マテリアル)

[.json メタデータ] → GltfMetadataManager → ModelMetadata
                      (モデルID, hideBoneKeys, textureSettings)

描画時:
  MixinItemRenderer → GltfObjectRendererRegistry → ActionArmsItemRenderer
    → RenderingContext 構築 (アニメーション状態含む)
    → GltfMeshRenderer.render()
       1. ボーン行列計算 (アニメーション → TRS → ローカル → ワールド → ボーン)
       2. 頂点処理 (インデックス → 基本座標 → モーフ → スキニング → 出力)
```

### データモデル（全て不変 record）

| クラス | 役割 |
|--------|------|
| `ProcessedGltfModel` | meshes, skins, materials, animations を保持 |
| `ProcessedMesh` | 頂点属性、インデックス、モーフターゲット |
| `ProcessedSkin` | ボーン階層（循環参照検出付き） |
| `ProcessedAnimation` | チャンネル群。ボーン別高速検索マップ |
| `ProcessedChannel` | キーフレーム補間（LINEAR/STEP/CUBICSPLINE） |
| `ProcessedMaterial` | PBR マテリアル（テクスチャ、係数、透明度モード） |
| `ProcessedBone` | 階層構造 + 逆バインド行列 |

### 性能最適化

- **GltfMemoryPool**: ThreadLocal な配列プール（float[], Matrix4f[], int[]）
- **直接ストリーミング**: 中間オブジェクト非生成で頂点をバッファに出力
- **メモ化キャッシュ**: GltfRenderLayer の RenderLayer 生成をキャッシュ
- **nameByChannels マップ**: ボーン別アニメーションチャンネル O(1) 検索

---

## 8. HUD システム

### サーバー側

- `ServerHudManager`: 毎ティック全プレイヤー監視、状態変更時のみ `HudStatePacket` 送信
- `LeverActionHudState`: マガジン内容 + チャンバー状態の DTO
- `BulletHitHudState`: ヒット/ヘッドショット/キルのフィードバック（色付き）

### クライアント側

- `ClientHudManager`: HudStatePacket 受信 → hudMap に格納（20tick で GC）
- `AAHudRenderer`: 弾薬スロット描画（縦配置）+ 動的クロスヘア（拡散角連動）

---

## 9. ネットワークパケット

| パケット | 方向 | 内容 |
|----------|------|------|
| `KeyInputPacket` | C2S | 全キー状態 (Map<Key, boolean>) |
| `AimPacket` | C2S | エイム状態 (boolean) |
| `ItemAnimationEventPacket` | S2C | アイテムUUID + アニメーションID + 時間 |
| `HudStatePacket` | S2C | HUD ID + NbtCompound |
| `RecoilPacket` | S2C | リコイル（ピッチ -5） |

---

## 10. プラットフォーム差異

| 機能 | Fabric | Forge |
|------|--------|-------|
| エントリ | ModInitializer / ClientModInitializer | @Mod + EventBus |
| 設定画面 | ModMenu API | ConfigScreenHandler |
| キーコンフリクト | TODO（未実装） | IKeyConflictContext |

---

## 11. 登録済みコンテンツ

| 種類 | ID | 詳細 |
|------|----|------|
| アイテム | `medium_caliber_bullet` | 中口径弾薬 |
| アイテム | `m1873` | M1873 レバーアクションライフル |
| エンティティ | `bullet` | 弾エンティティ（0.05×0.05、追跡4チャンク） |
| ダメージタイプ | `bullet` | カスタム弾丸ダメージ |
| サウンド | `item.gun.lever_action.*` | cycle/reload/fire/dry_fire |

---

## 12. 設計上の特徴

### 強み
- **コンポーネントベース**: 銃メカニクスを Item/Component/Data の3層に分離。新しい銃タイプ追加が容易
- **ラグ補償**: 環状バッファ + 線形補間による精度の高い衝突判定
- **glTF 描画**: メモリプール・直接ストリーミング・キャッシュによる高性能描画
- **差分同期**: HUD 状態の変更検出で帯域幅を最適化
- **イベント消費パターン**: キー入力の重複処理を防止

### 懸念点
- `LeverActionGunComponent` の複数 boolean フラグによる状態表現 → enum State 型のほうが明確になる可能性
- `MagazineComponent.getBullets()` が内部 LinkedList を直接公開
- `MagazineData.allowBullet` (Predicate) はシリアライズ不可
- `ItemUniqueManager.uniqueCheck()` の再帰（理論上の無限ループ可能性）
- Fabric 側の `KeyRegisterCallbackImpl` が未実装

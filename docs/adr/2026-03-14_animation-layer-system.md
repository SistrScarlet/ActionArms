# AnimationLayer システム設計（Unity Playable API シンプル版）

## ステータス

承認済み

## コンテキスト

現状のアニメーションシステムは `AnimationState[]` を後勝ち上書き（`animationOverwrite`）で適用する単純な構造。M1873 では問題なく動作しているが、SAA のシリンダー描画で以下が必要になった:

- **シリンダー回転**: firingIndex に応じた回転角をプログラム的に制御
- **薬室ごとの弾丸表示/非表示**: LOADED/SPENT/EMPTY でボーンの visibility 切替
- **ゲート開閉・ハンマーの状態ポーズ**: 1フレーム固定アニメーション

現状の仕組みでも個別に対応は可能だが、制御コードが ActionArmsItemRenderer に散らばり品質が低下する。既存のゲームエンジン（Unity Playable API）の概念を参考に、M1873 と SAA に必要十分な機能のみを持つシンプルなレイヤーシステムを導入する。

## 決定

### AnimationLayer sealed interface（2種）

```java
sealed interface AnimationLayer {
    int priority();

    // glTF アニメーションクリップ再生（現状の AnimationState 相当）
    record Clip(String animationName, float seconds, boolean looping, int priority)
        implements AnimationLayer {}

    // ゲーム状態からボーンの TRS をプログラム的に計算
    record Procedural(String boneName, BoneTRSSupplier supplier, int priority)
        implements AnimationLayer {}
}
```

### BoneTRS ラッパー

float[] の内部レイアウト（offset+3 が rotation.x 等）を隠蔽する:

```java
class BoneTRS {
    void setTranslation(float x, float y, float z);
    void setRotation(Quaternionf q);
    void setScale(float x, float y, float z);
}

@FunctionalInterface
interface BoneTRSSupplier {
    void apply(BoneTRS trs);
}
```

set されたチャンネルだけ書き込み、触らないチャンネルはデフォルト値を維持する。

### 優先度スキーム

| Priority | 用途 | 例 |
|----------|------|-----|
| 0 | アイドルループ | idle, idle_aiming |
| 10 | 状態ポーズ（1フレーム Clip） | hammerReady, leverDown, gateOpen |
| 20 | プログラム的制御 | シリンダー回転（Procedural） |
| 30 | ワンショットクリップ | fire, cock, eject, cycle |

低い priority が先に適用され、高い priority が上書きする。各レイヤーは自分が触るボーンだけ上書き（既存の animationOverwrite と同じ挙動）。

### 弾丸表示/非表示

既存の `hideBones` システムをそのまま使用。SAAItemRenderer が SAAHudState の chamberStates を参照し、動的にボーン名を追加:

```java
// LOADED でなければ弾丸ボーンを非表示
builder.addHideBone("bullet_" + i);
// SPENT でなければ薬莢ボーンを非表示
builder.addHideBone("cartridge_" + i);
```

### レンダラー構成

- `ActionArmsItemRenderer` — M1873 用（既存、Layer API に移行）
- `SAAItemRenderer extends ActionArmsItemRenderer` — SAA 用（新規）
- 登録: `ActionArmsClient` で `colt_saa` に `SAAItemRenderer::new` を指定（既存の仕組み）

### シリンダー角度の補間状態

`SAAItemRenderer` のフィールドに per-UUID Map で保持。AAHudRenderer と同じ prevRotation/currentRotation/targetRotation パターン。補間ロジックは static メソッドに切り出してテスト可能にする。

## 根拠

### 却下した代替案

**Pose レイヤー（特定ボーンの TRS を直接指定）**
- 値の出所が不明確（コードにハードコード？glTF から抽出？）
- 固定ポーズは 1フレーム Clip で代替可能、動的値は Procedural で代替可能
- → Clip と Procedural の2種で完全にカバーできるため不要

**Clip のボーンマスク**
- fire アニメーションが cylinder ボーンを含む場合の衝突を防げる
- 現時点ではモデリング側の制約（fire アニメに cylinder キーフレームを入れない）で対応可能
- → YAGNI。必要になったら Clip にフィールド追加で自然に拡張可能

**hideBones の AnimationLayer 統合**
- hideBones を priority 40 の Procedural（scale=0）として統合する案
- hideBones はメタデータ JSON 駆動の静的な仕組み、AnimationLayer は動的な制御で責務が異なる
- → 分離のまま維持

**完全な Unity Playable API 再現**
- AnimationMixerPlayable（ウェイトブレンド）、AnimationLayerMixerPlayable（レイヤー合成）等
- M1873 と SAA のユースケースには過剰
- → 必要十分な Clip + Procedural の2種に絞る

### 将来の拡張パス

- **クリップ間クロスフェード**: AnimationLayer にウェイトフィールドを追加し、computeAnimationData で lerp/slerp ブレンド
- **ボーンマスク**: Clip に `Set<String> targetBones` / `Set<String> excludeBones` を追加
- **加算ブレンド**: BlendMode enum（OVERRIDE/ADDITIVE）を AnimationLayer に追加

いずれも既存の Clip/Procedural 構造を壊さずフィールド追加で対応可能。

## 影響

### コード規模見積もり

| ファイル | 種別 | 行数目安 |
|---------|------|---------|
| AnimationLayer.java | 新規 | ~50 |
| BoneTRS.java / BoneTRSSupplier.java | 新規 | ~40 |
| SAAItemRenderer.java | 新規 | ~120 |
| SAAItemRendererState.java | 新規 | ~60 |
| RenderingContext.java | 変更 | ~30 |
| GltfMeshRenderer.java | 変更 | ~80 |
| ActionArmsItemRenderer.java | 変更 | ~20 |
| ActionArmsClient.java | 変更 | ~5 |
| **合計** | | **~400行** |

### 段階的実装計画

1. **Phase 1**: AnimationLayer 型定義 + RenderingContext 拡張（既存動作に影響なし）
2. **Phase 2**: GltfMeshRenderer を Layer 対応にリファクタ（M1873 の動作変更なし）
3. **Phase 3**: SAAItemRenderer 新規作成（シリンダー回転 + 弾丸表示/非表示）
4. **Phase 4**: M1873 を Layer API に移行（任意のクリーンアップ）

Phase 1→2→3 は順序依存。Phase 4 は独立。

### テスト可否

| 領域 | テスト可否 | 理由 |
|------|-----------|------|
| AnimationLayer の優先度ソート | 可能 | 純粋なデータ操作 |
| BoneTRS / BoneTRSSupplier | 可能 | float[] 操作のみ |
| シリンダー角度補間ロジック | 可能 | 数学のみ |
| GltfMeshRenderer.computeAnimationData | 条件付き | ProcessedSkin/Bone の手動構築が必要 |
| SAAItemRenderer.createRenderContext | 困難 | MC クライアント依存 |
| 視覚的正しさ | ゲーム内のみ | レンダリング結果の目視確認 |

### 変更対象の主要ファイル

- `common/src/main/java/net/sistr/actionarms/client/render/gltf/renderer/GltfMeshRenderer.java`
- `common/src/main/java/net/sistr/actionarms/client/render/gltf/renderer/RenderingContext.java`
- `common/src/main/java/net/sistr/actionarms/client/render/gltf/renderer/ActionArmsItemRenderer.java`
- `common/src/main/java/net/sistr/actionarms/client/ActionArmsClient.java`
- `common/src/main/java/net/sistr/actionarms/hud/SAAHudState.java`（データソース）

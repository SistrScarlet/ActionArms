# ActionArms 実装調査パターン集

## 概要

このドキュメントは、ActionArms MODプロジェクトにおける実装調査の具体的なパターンとワークフローを提供します。知識の泉を活用した効率的な調査手法を実例とともに示します。

## 基本調査フロー

### 調査の5段階

```
1. 概要把握 (files-index.json)
2. カテゴリ調査 (common/*.json)
3. 依存関係追跡 (dependencies)
4. 実コード確認 (実ファイル)
5. 詳細実装調査 (メソッド・ロジック)
```

## 調査パターン詳細

### パターン1: 新システム理解調査

#### 対象場面
- 新しく参加した開発者のシステム理解
- 既存システムの全体像把握
- システム間の関係性理解

#### 具体例: glTFレンダリングシステムの理解

**Step 1: 概要把握**
```bash
# files-index.jsonで全体像を確認
"gltf.json": "glTFモデル描画システム（副作用ゼロ設計完全対応）"
```

**Step 2: 主要コンポーネント特定**
```json
// gltf.jsonから重要度の高いファイルを抽出
"GltfRenderer.java": {"importance": "high"},
"DirectProcessor.java": {"importance": "high"},  
"RenderingContext.java": {"importance": "high"}
```

**Step 3: アーキテクチャの理解**
```
GltfModelManager (リソース管理)
    ↓
GltfModelConverter (変換処理)
    ↓
ProcessedGltfModel (変換済みデータ)
    ↓
GltfRenderer (描画実行)
    ↓
DirectProcessor (最適化処理)
```

**Step 4: 実装状況の把握**
- DirectProcessor: 中間オブジェクト100%削除完了
- RenderingContext: イミュータブル設計完了
- マテリアル対応: 5種類テクスチャ対応完了

#### 期待される成果
- システム全体の理解度80%達成
- 主要クラスの役割把握
- 実装状況と技術的特徴の理解

### パターン2: 機能連携調査

#### 対象場面
- 複数システム間のデータフロー調査
- 機能追加時の影響範囲確認
- バグの波及範囲特定

#### 具体例: エイム機能とHUD同期システムの連携

**Step 1: エントリポイント特定**
```json
// client.jsonでクライアント側エントリを確認
"ClientAimManager.java": {
  "desc": "クライアント側エイム管理とトグル・プッシュ両対応",
  "dependencies": ["AimPacket.java", "HasAimManager.java"]
}
```

**Step 2: データフロー追跡**
```
ClientAimManager (クライアント)
    ↓ AimPacket.sendC2S()
MixinServerPlayerEntity (サーバー)
    ↓ AimManager.setAiming()
ServerHudManager (サーバー)
    ↓ HudStatePacket.sendS2C()
ClientHudManager (クライアント)
    ↓ AAHudRenderer.render()
HUD表示更新
```

**Step 3: 実装詳細の確認**
```java
// 実際のコード例
// ClientAimManager.java
public void setAiming(boolean aiming) {
    if (this.aiming != aiming) {
        this.aiming = aiming;
        AimPacket.sendC2S(aiming); // サーバーに同期
    }
}
```

**Step 4: 同期タイミングの理解**
- エイム状態変更: 即座に同期
- HUD更新: 変化時のみ送信（効率化）
- タイムアウト: 20tick（1秒）で古い状態クリーンアップ

#### 期待される成果
- リアルタイム同期システムの理解
- ネットワーク効率化手法の把握
- マルチプレイヤー対応の実装方法理解

### パターン3: コンポーネントシステム調査

#### 対象場面
- コンポーネントベースアーキテクチャの理解
- 新しいコンポーネント設計
- 既存コンポーネントの拡張

#### 具体例: レバーアクション銃コンポーネントシステム

**Step 1: コンポーネント構造の把握**
```json
// item.jsonでコンポーネントシステムを確認
"LeverActionGunComponent.java": {
  "interfaces": ["FireTrigger", "CyclingLever", "Reloadable"],
  "dependencies": ["Chamber.java", "MagazineComponent.java"]
}
```

**Step 2: データ流れの理解**
```
LeverActionGunDataType (定義)
    ↓
LeverActionGunComponent (状態管理)
    ↓
Chamber → Cartridge → BulletComponent (弾薬管理)
    ↓
MagazineComponent (マガジン管理)
```

**Step 3: 操作フローの確認**
```java
// 発射フロー
1. trigger() -> FireTrigger実装
2. Chamber.shoot() -> 弾丸発射
3. サウンド再生 -> LeverActionPlaySoundContext
4. アニメーション -> ItemAnimationManager
```

**Step 4: NBT永続化の理解**
```java
// コンポーネントの保存・読み込み
public void writeToNbt(NbtCompound nbt) {
    // leverDown, cycling, hammerReady等の状態保存
}
```

#### 期待される成果
- コンポーネントベース設計の理解
- 状態管理とNBT永続化の手法
- インターフェース分離の利点理解

### パターン4: パフォーマンス調査

#### 対象場面
- パフォーマンス問題の特定
- 最適化手法の理解
- メモリ使用量の改善

#### 具体例: glTFレンダリングの最適化調査

**Step 1: 最適化ポイントの特定**
```json
// recent_major_changesで最適化履歴を確認
"2025-06-05": {
  "title": "中間オブジェクト完全削除による最大効率化完了",
  "performance_benefits": [
    "アロケーション: 100% → 5-10%",
    "メモリ使用量: 100% → 30-50%"
  ]
}
```

**Step 2: 最適化技法の理解**
```
従来: データ → 中間オブジェクト → 描画
最適化後: データ → 直接描画
```

**Step 3: 具体的実装の確認**
```java
// DirectProcessor.java
public void renderMeshDirect(ProcessedMesh mesh) {
    // 中間オブジェクトを作らず直接描画
    // メモリプールを活用してアロケーション削減
}
```

**Step 4: 効果の測定**
```
削除された中間オブジェクト:
- ComputedBoneMatricesData
- ComputedTRSData  
- ComputedVertexData
```

#### 期待される成果
- メモリ効率化手法の理解
- 中間オブジェクト削除パターンの習得
- ThreadLocalメモリプールの活用方法

### パターン5: バグ調査・デバッグ

#### 対象場面
- 機能不具合の原因特定
- 動作不正の調査
- 既知問題の影響範囲確認

#### 具体例: HUD表示の不具合調査

**Step 1: 問題箇所の特定**
```json
// 問題: HUDが表示されない
// AAHudRenderer.javaを知識の泉で確認
"AAHudRenderer.java": {
  "dependencies": ["LeverActionHudState.java", "ClientHudManager.java"]
}
```

**Step 2: データフロー確認**
```
ServerHudManager (データ作成)
    ↓ 状態変化時のみ送信
HudStatePacket (ネットワーク)
    ↓ NBT形式で送信
ClientHudManager (受信処理)
    ↓ 20tickタイムアウト管理
AAHudRenderer (描画実行)
```

**Step 3: 最近の変更確認**
```json
// recent_major_changesで関連する変更を確認
"2025-06-13": {
  "changes": [
    "HudState構造化・分離 - AAHudRenderer.HudStateから独立クラス化"
  ]
}
```

**Step 4: 実装詳細の調査**
```java
// 考えられる問題箇所
1. ServerHudManager: 状態変化の検出漏れ
2. HudStatePacket: NBTシリアライゼーション問題
3. ClientHudManager: タイムアウト処理の問題
4. AAHudRenderer: 描画条件の問題
```

#### 期待される成果
- 問題の根本原因特定
- デバッグ効率の向上
- 類似問題の予防策理解

## 調査効率化のコツ

### 1. 知識の泉活用法

```
概要把握: files-index.json → 5分
詳細調査: common/*.json → 15分
実装確認: 実際のコード → 30分
総時間: 50分（従来の1/3）
```

### 2. 優先度の付け方

```
importance: high → 最優先で理解
recent_major_changes → 最新の変更を優先確認
dependencies → 影響範囲の把握
```

### 3. 効率的な読み方

```
1. desc → 目的の理解
2. key_features → 主要機能の把握  
3. dependencies → 関連システムの特定
4. note → 制限事項・今後の予定
```

## まとめ

この実装調査パターン集により、ActionArms MODプロジェクトの複雑なシステムを効率的に理解できます。知識の泉を起点とした体系的なアプローチにより、調査時間を大幅に短縮し、より深い理解を得ることができます。

**重要なポイント**:
1. **段階的アプローチ**: 概要→詳細の順序を守る
2. **依存関係重視**: システム間の関係を常に意識
3. **実装状況確認**: 完成度と制限事項の把握
4. **最新情報優先**: recent_major_changesの活用

---
*効率的な実装調査により、開発生産性を最大化*
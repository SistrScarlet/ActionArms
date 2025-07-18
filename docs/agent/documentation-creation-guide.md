# ActionArms ドキュメント作成ガイド

## 概要

このガイドは、ActionArms MODプロジェクトにおけるドキュメント作成の標準化と効率化を目的として作成されています。知識の泉（Knowledge Spring）の調査結果を踏まえ、実装調査からドキュメント化までの包括的な手法を提供します。

## 基本方針

### 1. 知識の泉を調査起点として活用

知識の泉は**実装調査の地図**として機能します。詳細な実装調査の前に、必ず知識の泉で全体像を把握してください。

```
調査の流れ:
知識の泉で概要把握 → 実コード確認 → 詳細調査 → ドキュメント更新
```

### 2. 階層的ドキュメント構造

```
Level 1: 知識の泉 (調査インデックス)
├── files-index.json (全体概要)
└── common/*.json (詳細カテゴリ)

Level 2: 実装調査ガイド (このドキュメント)
├── 調査パターン集
├── アーキテクチャ図
└── コードサンプル集

Level 3: 実装詳細 (実コード + コメント)
├── 実際のJavaファイル
├── 実装手順書
└── トラブルシューティング
```

## 実装調査パターン集

### パターン1: システム全体把握調査

**使用場面**: 新しいシステムの理解、システム間連携の把握

**手順**:
1. `files-index.json`で全体像を把握
2. `key_features`で主要機能を特定
3. `importance: high`のファイルを優先確認
4. 依存関係を辿って関連システムを把握

**例**: glTFレンダリングシステムの全体把握
```json
// files-index.jsonから開始
"gltf.json": "glTFモデル描画システム（副作用ゼロ設計完全対応）"

// gltf.jsonで詳細確認
"GltfRenderer.java": {
  "importance": "high",
  "key_features": ["DIRECT/COPYLESS/STANDARDの3つの描画モード"],
  "dependencies": ["DirectProcessor", "RenderingContext"]
}
```

### パターン2: 機能間連携調査

**使用場面**: 複数システムの連携調査、データフローの把握

**手順**:
1. 対象機能の`dependencies`を確認
2. 逆引きで影響を受けるファイルを特定
3. ネットワーク通信の確認（network.json）
4. 実際のデータフローを追跡

**例**: エイム機能とHUD同期の関係
```
AimManager → AimPacket → ServerHudManager → HudStatePacket → ClientHudManager
```

### パターン3: 新機能開発調査

**使用場面**: 新機能の設計、類似機能の参考調査

**手順**:
1. 類似機能を知識の泉で検索
2. 実装パターンの抽出
3. 必要な依存関係の特定
4. 既存システムへの影響評価

**例**: 新しい銃タイプの追加
```
参考: LeverActionGunItem → コンポーネントシステム → レジストリ登録
```

### パターン4: バグ調査・デバッグ

**使用場面**: 不具合の原因調査、動作不正の特定

**手順**:
1. 問題の発生箇所を知識の泉で特定
2. 関連する`dependencies`を確認
3. `recent_major_changes`で最近の変更を確認
4. 実コードでの詳細調査

**例**: HUD表示の不具合調査
```
AAHudRenderer → LeverActionHudState → ServerHudManager → HudStatePacket
```

## ドキュメント作成標準

### 1. 知識の泉エントリの作成

**必須フィールド**:
```json
{
  "FileName.java": {
    "desc": "ファイルの目的と主要機能を1行で説明",
    "importance": "high/medium/low",
    "last_accessed": "2025-06-19",
    "key_features": ["具体的な機能", "メソッド名", "重要な特徴"],
    "dependencies": ["依存ファイル", "使用インターフェース"],
    "note": "実装状況、制限事項、今後の拡張予定"
  }
}
```

**記述ガイドライン**:
- `desc`: 動詞を含めて具体的に記述
- `key_features`: メソッド名や数値も含める
- `dependencies`: 直接的な依存関係のみ記載
- `note`: 実装の完成度を明記

### 2. アーキテクチャ図の作成

**対象システム**:
- 複数ファイルにまたがる複雑なシステム
- データフローが重要なシステム
- 依存関係が複雑なシステム

**作成形式**:
```
Mermaid記法を使用:
graph TD
    A[ClientAimManager] --> B[AimPacket]
    B --> C[ServerPlayerEntity]
    C --> D[AimManager]
```

### 3. コードサンプル集の整備

**対象パターン**:
- コンポーネントシステムの使用方法
- ネットワークパケットの送受信
- Mixinクラスの実装パターン

**形式**:
```java
// 目的: エイム状態の取得
public void example() {
    // 1. プレイヤーエンティティからAimManagerを取得
    AimManager aimManager = ((HasAimManager) player).getAimManager();
    
    // 2. エイム状態の確認
    boolean isAiming = aimManager.isAiming();
}
```

## 更新と保守の仕組み

### 1. 更新タイミング

**必須更新**:
- 新しいJavaファイルの追加時
- 重要な機能変更時
- システム間の依存関係変更時

**推奨更新**:
- 週1回の定期更新
- 大きな機能追加完了時
- バグ修正後

### 2. 品質保証

**確認項目**:
- [ ] 実コードと知識の泉の整合性
- [ ] 依存関係の正確性
- [ ] 実装状況の最新性
- [ ] 記述の明確性

### 3. 自動化の検討

**将来的な改善案**:
- コード解析による自動更新
- 依存関係の自動抽出
- 実装完了度の自動判定

## 実践的な活用例

### 新規開発者のオンボーディング

```
Day 1: files-index.jsonで全体把握
Day 2: 主要システム (importance: high) の理解
Day 3: 実装調査パターンの練習
Day 4: 実際の開発タスクへの適用
```

### 機能追加の計画

```
1. 類似機能の調査 (知識の泉)
2. 依存関係の特定
3. 影響範囲の評価
4. 実装設計
5. 知識の泉の更新
```

### バグ修正の効率化

```
1. 問題箇所の特定 (知識の泉)
2. 関連システムの確認
3. 最近の変更履歴の確認
4. 実コード調査
5. 修正と知識の泉更新
```

## まとめ

このガイドは、ActionArms MODプロジェクトにおけるドキュメント作成と実装調査の効率化を目的としています。知識の泉を起点とした体系的な調査手法により、開発生産性の向上と知識の共有を実現します。

**重要な原則**:
1. 知識の泉は**地図**、実コードは**目的地**
2. 構造化された情報整理による効率化
3. 継続的な更新による品質維持
4. 実践的なパターン活用による学習促進

---
*ActionArms「知識の泉」- プロジェクトと共に成長する知見データベース*
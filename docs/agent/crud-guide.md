# エージェント用「知識の泉」操作指示書

## 概要
ActionArms MODプロジェクトの「知識の泉」（ファイル知見管理システム）の操作方法

## ファイル構造
```
docs/agent/
├── files-index.json     # 全体概要
├── files.json          # 旧システム（参考用）
└── common/
    ├── root.json        # ActionArms.java
    ├── client.json      # クライアント・キーバインド
    ├── config.json      # MOD設定
    ├── item.json        # 銃アイテム・コンポーネント
    ├── network.json     # ネットワーク通信
    ├── setup.json       # アイテム登録
    ├── gltf.json        # glTFモデル描画システム（副作用ゼロ設計完全対応）
    ├── component.json   # 移行完了ファイル（全クラスがitem/componentに統合済み）
    └── entity.json      # エンティティシステム（弾丸エンティティ）
```

## CRUD操作

### 🔍 Read (読み取り)

#### 全体概要を確認
```
read_file: docs/agent/files-index.json
```

#### 特定カテゴリの詳細を確認
```
read_file: docs/agent/common/{カテゴリ名}.json
```

#### 複数ファイルを一度に確認
```
read_multiple_files: [
  "docs/agent/files-index.json",
  "docs/agent/common/item.json"
]
```

### ✏️ Create (新規追加)

#### 新しいJavaファイルを追加する場合

1. **実際のJavaファイルを読む**
```
read_file: {新しいファイルのパス}
```

2. **適切なカテゴリを判断**
- `src/main/java/net/sistr/actionarms/` 直下 → root.json
- `client/` 配下 → client.json  
- `config/` 配下 → config.json
- `item/` 配下 → item.json
- `network/` 配下 → network.json
- `setup/` 配下 → setup.json
- `gltf/` 配下 → gltf.json
- `entity/` 配下 → entity.json
- 移行完了ファイル → component.json

3. **該当カテゴリファイルに追加**
```json
{
  "既存のファイル": { ... },
  "新しいファイル.java": {
    "desc": "ファイルの説明",
    "importance": "high/medium/low",
    "last_accessed": "2025-06-14",
    "key_features": ["特徴1", "特徴2"],
    "dependencies": ["依存ファイル"],
    "note": "重要な備考"
  }
}
```

#### 新しいカテゴリディレクトリが追加された場合

1. **新しいJSONファイルを作成**
```
write_file: docs/agent/common/{新カテゴリ名}.json
```

2. **files-index.jsonを更新**
```json
{
  "structure": {
    "common/": {
      "detail_files": {
        "新カテゴリ名.json": "カテゴリの説明"
      }
    }
  }
}
```

### 🔄 Update (更新)

#### 既存ファイル情報の更新

1. **現在の情報を読む**
```
read_file: docs/agent/common/{カテゴリ}.json
```

2. **ファイルを編集**
```
edit_file: docs/agent/common/{カテゴリ}.json
edits:
  - oldText: "古い情報"
    newText: "新しい情報"
```

#### よく更新する項目
- `last_accessed`: ファイルにアクセスした日付
- `desc`: ファイルの説明（機能追加時）
- `key_features`: 新機能追加時
- `dependencies`: 依存関係変更時
- `note`: 重要な変更があった場合

### 🗑️ Delete (削除)

#### ファイルが削除された場合

1. **該当カテゴリファイルを読む**
```
read_file: docs/agent/common/{カテゴリ}.json
```

2. **該当エントリを削除**
```
edit_file: docs/agent/common/{カテゴリ}.json
edits:
  - oldText: '"削除対象ファイル.java": { ... },'
    newText: ''
```

#### カテゴリ全体が削除された場合

1. **カテゴリファイルを削除**（通常は不要、コメントアウト推奨）

2. **files-index.jsonから該当部分を削除**

## 🎯 使用パターン

### パターン1: 新しいJavaファイルが追加された時
```
1. read_file で実際のJavaファイルを読む
2. 内容を理解して適切なカテゴリを判断
3. edit_file で該当カテゴリJSONに情報追加
4. files-index.jsonの last_updated を更新
```

### パターン2: 既存ファイルを編集する前
```
1. 知識の泉でファイル情報を検索
2. read_file で詳細情報を確認  
3. 編集作業実行
4. edit_file で last_accessed を更新
```

### パターン3: プロジェクト全体を把握したい時
```
1. read_file で files-index.json を確認
2. read_multiple_files で必要なカテゴリを一括取得
3. 重要度 "high" のファイルを優先確認
```

### パターン4: 特定機能の実装を探す時
```
1. files-index.json の key_features で概要把握
2. 関連カテゴリのJSONファイルで詳細確認
3. dependencies で関連ファイルを追跡
```

## 📝 記述ルール

### importance の基準
- **high**: MODの核となる機能、頻繁に編集する、エイム・HUD・銃システムなど
- **medium**: 重要だが編集頻度は中程度、ネットワーク・設定など
- **low**: ユーティリティ、テスト、例示コード

### desc の書き方
- 1行で要約、具体的に
- 「〜を管理」「〜の実装」「〜システム」など動詞を含める
- 現在の実装状況も記載（完全実装/部分実装/空実装なら明記）
- 例：「エイム状態管理システム実装（アイテム切り替え時自動解除）」

### key_features の書き方
- 箇条書きで主要機能
- メソッド名や重要な変数名を含める
- 完成した機能は明確に記載
- 将来の拡張予定も記載
- 例：「トグル・プッシュ両対応」「リアルタイム同期」

### dependencies の記載方法
- 直接的な依存関係を記載
- インターフェース実装関係も含める
- Mixinの場合は対象クラスも記載
- ネットワークパケットの場合は送受信クラスを記載

## 🔧 ActionArms特有の分類

### 重要システム（高優先度で知識の泉更新）
- **銃システム**: LeverActionGun*, FireTrigger, CyclingLever, Reloadable
- **エイム機能**: AimManager, ClientAimManager, MixinPlayerEntity
- **HUD描画**: AAHudRenderer, *HudState, *HudManager  
- **キー入力**: KeyInputManager, GunController, AAKeys
- **描画系**: MixinHeldItemRenderer, MixinItemRenderer, gltf/*

### ネットワーク系
- **パケット**: *Packet.java, Networking.java
- **同期対象**: エイム状態、HUD状態、キー入力、アニメーション

### ユーティリティ系  
- **弾薬管理**: InventoryAmmoUtil
- **エンティティ**: BulletEntity*, entity/util/*
- **音響**: *SoundContext, Registration.java（サウンド部分）

## ⚠️ 注意点

- **トークン効率**: 必要な時だけ詳細ファイルを読む
- **整合性**: 依存関係の記載は両方向で行う
- **日付更新**: アクセス時は必ず last_accessed を更新
- **重要度**: プロジェクトの進行に合わせて見直し
- **新機能追加時**: key_features と recent_major_changes を必ず更新
- **Mixin系**: 対象クラスと注入機能を明確に記載
- **インターフェース**: 実装クラスとの関係を dependencies に記載

## 🚀 ActionArms知識の泉活用のコツ

### 効率的な情報検索
1. **概要把握**: files-index.json → key_features で全体像把握
2. **詳細調査**: 関連カテゴリのJSONで specific な情報取得  
3. **依存追跡**: dependencies を辿って関連ファイル発見
4. **実装確認**: importance "high" のファイルを優先的に確認

### 新機能開発時
1. **既存調査**: 類似機能の実装を知識の泉で検索
2. **依存確認**: 新機能が依存する既存システムを特定
3. **影響範囲**: dependencies で影響を受けるファイルを把握
4. **実装後**: 知識の泉に新機能情報を追加

---
*ActionArms「知識の泉」- プロジェクトと共に成長する知見データベース*
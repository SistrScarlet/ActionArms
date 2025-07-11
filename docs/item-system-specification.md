# ActionArms - アイテム体系仕様書

## 1. 基本コンセプト
- 銃器の機構と操作の心地よさを重視
- 製造から使用まで一貫したゲーム体験
- リアリティと利便性のバランス

## 2. アイテム分類

### 2.1 銃器
**基本特性**
- 消耗品（射撃で耐久値減少、ゼロで故障）
- 耐久値低下でジャム確率上昇
- 弾薬装填方式：単発装填 or 弾倉式

**分類システム（タグベース）**
```
基本分類: 拳銃/ライフル/ショットガン/特殊
口径タグ: 小口径/中口径/大口径/特大口径
機構タグ: オート/セミオート/マニュアル/シングル
特性タグ: 精密/制圧/近接/遠距離
```

**性能分類**
- 連射武器：高DPS、高コスト、低威力、制御困難
- 単発武器：低DPS、低コスト、高威力、制御容易
- オートマチック：操作簡単、高製造コスト、高故障率
- マニュアル：操作複雑、低製造コスト、高信頼性

### 2.2 弾薬システム（口径ベース）
**分類**
- 小口径、中口径、大口径、特大口径
- 分かりやすさのため簡略化した名称
- アイコンデザインで識別性向上

**特性**
- ダメージは弾薬基準（口径で決定）
- 連射速度に補正をかける
- 耐久消耗値基準を持つ
- スタック可能

**弾種バリエーション**
- 通常弾、徹甲弾、炸裂弾など
- 材質による差別化（鉛・銅・鉄・特殊合金）

### 2.3 弾倉システム（汎用・専用混在）
**基本仕様**
- 同口径なら基本的に汎用使用可能
- 高性能銃器の一部は専用弾倉が必要
- 弾薬よりスタック数制限

**容量・材質**
- 容量：5発/10発/30発/ドラム(100発)
- 材質：木製/鉄製/合金製
- 材質で重量・耐久性・装填速度が変化

## 3. 操作システム

### 3.1 基本操作
**銃器別操作**
- オートマチック：リロードキーで弾倉交換
- レバーアクション：リロードキーで弾薬挿入 + 射撃後コッキング必須
- マニュアル系：タイミング操作で時間短縮可能

**弾薬選択**
- 弾選択キーでインベントリ内弾薬を指定

### 3.2 タクティカルリロード（弾薬ポーチシステム）
**仕様**
- 専用アイテム「弾薬ポーチ」を装備
- 中途半端な弾倉は一時的にポーチに格納
- 後で整理・統合可能
- ポーチ自体をアップグレード要素として活用（容量・自動整理機能）

### 3.3 操作簡略化パーツ
**機能**
- パーツ装着で手動操作を自動化
- 例：レバーアクションの射撃後コッキング自動化
- トレードオフ：操作時間は手動より長い

## 4. 性能システム

### 4.1 パラメータ依存関係
- **ダメージ**：弾薬基準（口径依存） + 銃器補正
- **連射速度**：銃器基準 + 弾薬補正
- **耐久消耗**：弾薬基準 + 銃器補正

### 4.2 耐久・故障システム（段階的劣化）
**劣化段階**
- 100% → 75%：性能維持
- 75% → 50%：軽微な性能低下
- 50% → 25%：明確な性能低下 + 稀なジャム
- 25% → 0%：頻繁なジャム + 大幅性能低下

**修理システム**
- 「メンテナンスキット」アイテムで一律回復
- 使用回数制限で完全修理不可
- 最大耐久値が徐々に減少 → 最終的に廃棄必要
- 経済循環の促進

## 5. 拡張システム

### 5.1 カスタマイズ
- 性能の微調整が可能（詳細未定）

### 5.2 強化システム
- 銃器補正値の強化
- エンチャント類似のランダム性高コストシステム
- 限界を目指すとコストが高く付く設計

## 6. 製造システム連携
- 銃・弾薬・弾倉の製造がゲーム要素として含まれる
- 口径ベース弾薬：「火薬 + 金属 + ケース」の組み合わせ
- タグシステムにより製造レシピの柔軟な設計が可能
- 材質・容量による弾倉の製造バリエーション

---
*作成日: 2025-05-27*
*バージョン: 1.0*
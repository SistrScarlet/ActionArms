# 銃状態管理 Level 1 リファクタ

## ステータス

承認済み

## コンテキスト

`LeverActionGunComponent` が 4 つの boolean フラグ（`hammerReady`, `leverDown`, `cycling`, `reloading`）と 5 つの float タイマーで状態を管理している。不正状態（`cycling=true && reloading=true`）が型レベルで防げず、`canXxx()` の条件が複雑で見通しが悪い。

テスト追加の過程で以下のバグが発見された:
1. `trigger()` がリロードをキャンセルしない
2. `cycleTick` 後半フェーズが `leverUpLength` ではなく `leverDownLength` を使用
3. `canShoot` と `canTrigger` の条件重複

これらは boolean フラグの組み合わせ管理に起因する構造的な問題。

## 決定

以下の 4 つの改善を実施する:

### 1. GunPhase enum 導入
`cycling`, `reloading`, `leverDown` を削除し、`GunPhase` enum + `phaseTimer` に統合。`hammerReady` は Phase と直交する概念のため独立フラグとして残す。

### 2. クールダウン統合
`fireCoolTime`, `cycleCoolTime`, `reloadCoolTime` を 1 つの `cooldownTime` に統合。同時に複数のクールダウンが走ることは構造上ないことを確認済み。

### 3. GunController の tryAction 抽出
3 回繰り返される「キー判定 → can → 実行 → killKey」パターンをメソッドに抽出。

### 4. setter 削除
外部から呼ばれていない setter を削除し、内部状態の不正操作を防止。

## 根拠

- **Level 2（State パターン）は見送り**: 現在レバーアクション銃 1 種のみ。クラス数が増えるデメリットに対して恩恵が限定的。2 種目追加時に導入する。
- **Phase enum**: switch 文で全状態を網羅でき、不正状態が型レベルで存在不可能になる。
- **クールダウン統合**: 同時に走らないことをコード追跡で確認済み。全 `canXxx()` が 3 つとも 0 を要求しており、構造的に重複不可能。
- **テストが安全網**: 46 件のブラックボックステストが既存の振る舞いを保証しているため、リファクタ中の回帰を即座に検出できる。

## 影響

- `LeverActionGunComponent` の内部構造が大きく変わるが、外部 IF（`FireTrigger`, `CyclingLever`, `Reloadable`）は維持
- NBT の read/write キーが変わる可能性がある（既存ワールドの互換性に注意）
- HUD 表示（`LeverActionHudState`）が参照する状態の取得方法が変わる

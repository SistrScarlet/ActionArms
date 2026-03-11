# コルト SAA（シングルアクションリボルバー）アクション設計

## 概要

2種目の銃としてコルト SAA（シングルアクションリボルバー）を追加する。
レバーアクションと大きく異なるメカニクスを持つため、Level 2 リファクタの判断材料となる。

## キー操作

| 状態 | FIRE | COCK | OPERATE（新規） | RELOAD |
|------|------|------|----------------|--------|
| IDLE | - | シリンダー回転+コック | ゲート開 | - |
| FULL_COCK | 射撃 | 何もしない | - | - |
| FULL_COCK+FIRE押下中 | - | ファニング（回転+コック+即射撃） | - | - |
| GATE_OPEN | - | 排莢（実弾/空薬莢→インベントリ） | ゲート閉 | 装填 or シリンダー回転 |

### OPERATE キー

- 汎用名。SAA ではローディングゲート開閉、他の銃（自動小銃等）ではマガジン脱着に使用
- 新規キーバインド追加が必要

### RELOAD キーの挙動（ゲート開放中）

優先順位:
1. 現在の薬室が空 + インベントリに弾がある → 装填
2. 上記以外 → シリンダー回転

### COCK キーの挙動（ゲート開放中）

- 現在の薬室に弾（実弾 or 空薬莢）がある → 排莢してインベントリに戻す
- 排莢後は自動回転

## 状態遷移

```
[IDLE] ──(COCK)──→ シリンダー回転 → [FULL_COCK] ──(FIRE)──→ 射撃 → [FIRE_COOLING] → [IDLE]
  │                                     │
  │                               (FIRE押下中+COCK)
  │                                ファニング連射
  │
  └──(OPERATE)──→ [GATE_OPEN] ──(OPERATE)──→ [IDLE]
                      │
                 ┌────┴────┐
                 │         │
              (COCK)    (RELOAD)
              排莢       装填/回転
                 │         │
                 └────┬────┘
                      ↓
                 自動回転 → [GATE_OPEN]
```

### 空撃ち

FULL_COCK で空薬室に当たった場合、空撃ち（ドライファイア）して IDLE に戻る。

## ファニング射撃

- FIRE キー押しっぱなし + COCK 連打
- 各 COCK で「シリンダー回転 → ハンマーコック → トリガー保持中なので即発射」
- クールダウンはコック速度と同一（専用パラメータなし）
- レバーアクションの連射がレバー操作律速なのに対し、SAA はコック速度律速

## データ構造

### Cylinder（シリンダー）

```java
public class Cylinder {
    private final Cartridge[] chambers; // 固定長6
    private int currentIndex;

    public void rotate() { currentIndex = (currentIndex + 1) % chambers.length; }
    public Optional<Cartridge> ejectCurrent() { ... }
    public boolean loadCurrent(BulletData bullet) { ... }
    public Optional<Cartridge> shootCurrent() { ... }
}
```

レバーアクションの Chamber(1発) + MagazineComponent(チューブ) に対し、
SAA は Cylinder が薬室と弾倉を兼ねる。

## レバーアクションとの比較

| 観点 | レバーアクション | SAA リボルバー |
|------|------------------|----------------|
| 薬室 | 1つ (Chamber) | 6つ (Cylinder) |
| 弾倉 | チューブマガジン (FIFO) | シリンダー自体が弾倉 |
| 射撃準備 | レバー操作で自動コック | 手動ハンマーコック |
| 排莢 | レバー操作で自動排莢 | 手動イジェクターロッド（COCK+ゲート開） |
| 装填 | 外部からマガジンに押し込む | ゲート開→1発ずつシリンダーに |
| 追加キー | なし | OPERATE キー |
| 速射 | レバー操作速度で律速 | ファニング（コック速度で律速） |

## その他仕様

- ゲート開放中の被弾/移動: 特に制限なし
- FULL_COCK 状態で再度 COCK: 何も起こらない
- 2発目空け安全装填: 再現しない（全弾装填可）
- 射撃後の自動シリンダー回転: なし（次の COCK 操作時に回転）

## 実装方針

独立実装方針。LeverActionGunComponent とコード重複しても構わず、SAA 固有の実装を書く。
コントローラー/コンポーネントの汎用化は 3 種目の武器追加時に判断する。

### 検討済みの設計判断

- **tick 統合（関数型）アプローチ**: 検討したが、2丁持ち時の入力調停・銃種ごとのコンテキスト差異で問題。不採用。
- **共通アクションインターフェース**: tick 統合と本質的に同じ。2種では汎用化の軸が定まらないため時期尚早。
- **現在のコントローラー/コンポーネント分離は妥当**: コントローラー=操作（入力調停）、コンポーネント=メカニズム（状態遷移）。
- **ファニング**: コンポーネントは cock() と fire() だけ持つ。コントローラーが「トリガー保持中+コック完了→即fire()」を行う。

## ステップ

1. OPERATE キーの追加（AAKeys, KeyInputManager, パケット）
2. Cylinder データ構造の実装
3. SAAGunComponent の実装（状態遷移・各アクション）
4. SAAGunItem の実装
5. Registration に登録
6. 動作確認・パラメータ調整

## 対象ファイル（新規作成予定）

- `common/.../item/component/SAAGunComponent.java`
- `common/.../item/SAAGunItem.java`
- `common/.../item/util/Cylinder.java`
- `common/.../input/AAKeys.java`（既存に OPERATE 追加）
- 各種パケット・Registration（既存に追加）

## 検証方法

- ユニットテスト: SAAGunComponent の状態遷移テスト
- ゲーム内: 射撃・コッキング・ゲート開閉・排莢・装填・ファニングの動作確認

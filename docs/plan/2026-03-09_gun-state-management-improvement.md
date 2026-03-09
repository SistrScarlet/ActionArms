# 銃周り状態管理の改善案

作成日: 2026-03-09

## 現状の分析

### 状態の全体像

`LeverActionGunComponent` が以下の状態を boolean フラグ × float タイマーの組み合わせで管理:

```
フラグ: hammerReady, leverDown, cycling, reloading (4個)
タイマー: cycleTime, reloadTime, fireCoolTime, cycleCoolTime, reloadCoolTime (5個)
サブコンポ: Chamber, MagazineComponent
```

### 暗黙の状態遷移ルール

フラグの組み合わせで「今何をしているか」が決まるが、明示されていない:

| 状態 | hammerReady | leverDown | cycling | reloading | 備考 |
|------|:-----------:|:---------:|:-------:|:---------:|------|
| 射撃可能 | true | false | false | false | + fireCoolTime==0 |
| 射撃後 | false | false | false | false | fireCoolTime > 0 |
| サイクル前半 | - | false | true | false | レバー下げ中 |
| サイクル後半 | false | true | true | false | レバー上げ中 |
| リロード中 | - | - | false | true | reloadTime > 0 |
| 各クールダウン | - | - | false | false | *CoolTime > 0 |

### 現状の問題点

1. **不正状態の存在**: `cycling=true && reloading=true` は不正だが、型レベルで防げない
2. **canXxx() の複雑な前提条件**: 全メソッドが 4-6 個の条件を AND で結合。判断基準がコードを読まないと分からない
3. **キャンセル可能性の暗黙知**: `cycleCancelableLength` / `reloadCancelableLength` でキャンセル判定しているが、どの操作がどの操作をキャンセルできるかがコード内に散在
4. **タイマーの多重化**: 「アクションのタイマー」と「クールダウンのタイマー」が同列に並んでいて、各タイマーの意味が混在
5. **GunController の God Lambda**: `tickGunComponent` が巨大なラムダ内で全操作を処理。キー入力判定 → canXxx → 実行 → killKey のパターンが3回繰り返される

---

## Level 1: 小手先の改善（現アーキテクチャ維持）

### 1-1. Phase enum の導入

現在の boolean 組み合わせを enum に集約し、不正状態を型レベルで排除する。

```java
public enum GunPhase {
    IDLE,           // 何もしていない
    FIRE_COOLING,   // 射撃後クールダウン
    CYCLE_DOWN,     // レバー下げ中
    CYCLE_UP,       // レバー上げ中（leverDown=true）
    CYCLE_COOLING,  // サイクル後クールダウン
    RELOADING,      // リロード中
    RELOAD_COOLING; // リロード後クールダウン

    public boolean isBusy() {
        return this != IDLE;
    }

    public boolean canInterrupt() {
        return this == CYCLE_DOWN || this == RELOADING;
    }
}
```

**変更点:**
- `LeverActionGunComponent` から `cycling`, `reloading`, `leverDown` を削除
- `GunPhase phase` + `float phaseTimer` に統合
- `hammerReady` は独立フラグとして残す（Phase と直交する概念）
- `canTrigger()` 等が `phase.canInterrupt()` + タイマー条件に簡素化

**メリット:** 状態を列挙できるので、switch 文で全状態を処理でき、漏れが生じにくい
**リスク:** 低い。内部リファクタのみで外部 IF は変わらない

### 1-2. クールダウンの統合

3つのクールダウンタイマー (`fireCoolTime`, `cycleCoolTime`, `reloadCoolTime`) を1つに統合。

```java
private float cooldownTime; // Phase 遷移時にセットされる汎用クールダウン
```

**根拠:** 同時に複数のクールダウンが走ることは構造上ありえない（1つの操作が終わってからクールダウンに入る）

### 1-3. GunController のキー処理パターン抽出

3回繰り返される「キー判定 → can → 実行 → killKey」パターンをメソッドに抽出:

```java
private boolean tryAction(
        KeyInputManager.Key key, int window,
        BooleanSupplier canAction, Supplier<Boolean> action) {
    if (!keyInputManager.isTurnPressWithin(key, window)) return false;
    if (!canAction.getAsBoolean()) return false;
    if (!action.get()) return false;
    keyInputManager.killTurnPressWithin(key, window);
    return true;
}
```

### 1-4. setter の削除

`setCycling()`, `setReloading()`, `setCycleTime()` 等の setter は外部から呼ばれていない。
削除して内部状態の不正な操作を防ぐ。getter のみ残す。

---

## Level 2: 抜本的な作り直し

### 2-1. State パターン（状態オブジェクト）

各状態を独立したクラスとして実装し、状態遷移を明示的にする。

```java
// 状態の基底
public sealed interface GunState {
    GunState tick(float delta, GunStateContext ctx);
    boolean canTransitionTo(GunAction action, GunStateContext ctx);
}

// 具体的な状態
public record IdleState(boolean hammerReady) implements GunState {
    @Override
    public GunState tick(float delta, GunStateContext ctx) {
        return this; // 変化なし
    }

    @Override
    public boolean canTransitionTo(GunAction action, GunStateContext ctx) {
        return switch (action) {
            case FIRE -> hammerReady && ctx.chamber().canShoot();
            case CYCLE -> true;
            case RELOAD -> ctx.magazine().canAddBullet();
        };
    }
}

public record CyclingDownState(float remaining) implements GunState {
    @Override
    public GunState tick(float delta, GunStateContext ctx) {
        float next = remaining - delta;
        if (next <= 0) {
            ctx.chamber().ejectCartridge().ifPresent(ctx::onEject);
            return new CyclingUpState(ctx.gunData().leverDownLength());
        }
        return new CyclingDownState(next);
    }

    @Override
    public boolean canTransitionTo(GunAction action, GunStateContext ctx) {
        return action == GunAction.FIRE
            && remaining <= ctx.gunData().cycleCancelableLength();
    }
}

// CyclingUpState, ReloadingState, CooldownState ... 同様
```

**GunAction enum:**
```java
public enum GunAction { FIRE, CYCLE, RELOAD }
```

**GunStateContext (状態が参照する外部リソース):**
```java
public record GunStateContext(
    LeverActionGunData gunData,
    Chamber chamber,
    MagazineComponent magazine,
    Runnable onEject, // 排莢コールバック
    ... // 他のコールバック
) {}
```

**状態遷移の管理:**
```java
public class GunStateMachine implements IComponent {
    private GunState state = new IdleState(false);
    private final Chamber chamber;
    private final MagazineComponent magazine;

    public void tick(float delta, GunStateContext ctx) {
        this.state = this.state.tick(delta, ctx);
    }

    public boolean tryAction(GunAction action, GunStateContext ctx) {
        if (!state.canTransitionTo(action, ctx)) return false;
        this.state = switch (action) {
            case FIRE -> handleFire(ctx);
            case CYCLE -> new CyclingDownState(ctx.gunData().leverUpLength());
            case RELOAD -> new ReloadingState(ctx.gunData().reloadLength());
        };
        return true;
    }
}
```

**メリット:**
- 各状態の責務が完全に分離。新しい状態（ジャム、ボルト操作等）の追加が安全
- `canTransitionTo()` が各状態クラス内にカプセル化。条件の見通しが良い
- sealed interface + switch で網羅性チェック可能
- 不正状態が型レベルで存在不可能

**デメリット:**
- クラス数が増える（7-8クラス）
- NBT シリアライズが複雑化（状態の種類 + パラメータの保存）
- 既存の `FireTrigger`, `CyclingLever`, `Reloadable` インターフェースとの整合

### 2-2. GunController の責務分離

現在 GunController は「全インベントリのスキャン」「UUID管理」「キー入力判定」「銃コンポーネント操作」を1メソッドで行っている。

```
GunController (現状)
  └── tickGunComponent() ← 全部入り

GunController (改善後)
  ├── GunInventoryScanner: インベントリスキャン + UUID管理
  ├── GunActionDispatcher: キー入力 → GunAction 変換
  └── GunStateMachine: 状態遷移 + tick
```

### 2-3. コンテキストオブジェクトの整理

現在、各メソッドに3-4個のコンテキスト引数が渡されている:

```java
// 現状: 引数が多い
trigger(playSoundContext, animationContext, fireContext)
tick(playSoundContext, cycleContext, reloadContext, timeDelta, active)
```

**改善案: 統合コンテキスト**

```java
public record GunTickContext(
    SoundPlayer soundPlayer,
    AnimationPlayer animationPlayer,
    BulletSpawner bulletSpawner,
    CartridgeEjector cartridgeEjector,
    AmmoSupplier ammoSupplier,
    float timeDelta
) {}
```

State パターンと組み合わせると、各状態が必要なコンテキストだけを使う形になる。

### 2-4. IComponent のデシリアライズ改善

現在は毎 tick で `new → read → 操作 → write` している（値型的な使い方）。

```java
// 現状: 毎tick read/write
IComponent.execute(leverAction.getGunComponent(), stack, gunComponent -> { ... });
```

**改善案:** コンポーネントをキャッシュし、dirty フラグで遅延書き込み:

```java
// GunController がコンポーネントインスタンスをキャッシュ
// tick 完了時に dirty な場合のみ write
private final Map<UUID, LeverActionGunComponent> componentCache;
```

ただし、NBT が外部から書き換えられる可能性（/data コマンド等）を考慮すると、現在の毎 tick read/write は安全。性能が問題にならない限り現状維持が無難。

---

## 推奨アプローチ

**段階的に進める:**

1. **まず Level 1 を全て適用** — Phase enum + クールダウン統合 + setter 削除 + tryAction 抽出
2. **その上で必要性を評価** — 新しい銃タイプ（ボルトアクション、セミオート等）を追加する際に Level 2 の State パターンが必要になるか判断
3. **Level 2 は銃タイプ追加時に導入** — 現在レバーアクション1種のみなので、State パターンの恩恵は限定的。2種目追加時に共通の状態遷移基盤として導入するのが自然

Level 1 だけでも、状態の見通しと保守性は大幅に改善される。

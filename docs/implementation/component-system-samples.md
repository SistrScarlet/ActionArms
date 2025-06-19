# ActionArms コンポーネントシステム実装サンプル集

## 概要

このドキュメントは、ActionArms MODのコンポーネントベースアーキテクチャの実装パターンを、実際のコードサンプルとともに解説します。知識の泉のitem.jsonから得られた情報を基に、実践的な実装手法を提供します。

## 基本コンポーネントパターン

### 1. IItemComponent 基本実装

#### 知識の泉からの情報

```json
// item.json より
"IItemComponent.java": {
  "desc": "アイテムコンポーネントシステムのインターフェース",
  "key_features": [
    "NBT読み書きメソッド",
    "静的executeメソッド（ComponentResult返り値）",
    "ComponentResult enum（NO_CHANGE/MODIFIED）"
  ]
}
```

#### 基本実装パターン

```java
public class CustomComponent implements IItemComponent {
    private boolean customState = false;
    private int customValue = 0;
    
    // NBT読み書きの実装
    @Override
    public void writeToNbt(NbtCompound nbt) {
        nbt.putBoolean("customState", customState);
        nbt.putInt("customValue", customValue);
    }
    
    @Override
    public void readFromNbt(NbtCompound nbt) {
        this.customState = nbt.getBoolean("customState");
        this.customValue = nbt.getInt("customValue");
    }
    
    // 静的操作メソッド（推奨パターン）
    public static ComponentResult execute(ItemStack stack, 
                                        ExecuteFunction<CustomComponent> function) {
        return IItemComponent.execute(stack, CUSTOM_COMPONENT, function);
    }
    
    // 読み取り専用アクセス
    public static Optional<CustomComponent> query(ItemStack stack) {
        return IItemComponent.query(stack, CUSTOM_COMPONENT);
    }
    
    // ゲッター・セッター
    public boolean isCustomState() { return customState; }
    public void setCustomState(boolean state) { this.customState = state; }
    
    public int getCustomValue() { return customValue; }
    public void setCustomValue(int value) { this.customValue = value; }
}
```

### 2. BaseItemComponent 継承パターン

```java
public class AdvancedComponent extends BaseItemComponent {
    private String name = "";
    private List<String> items = new ArrayList<>();
    
    @Override
    public void writeToNbt(NbtCompound nbt) {
        super.writeToNbt(nbt);
        nbt.putString("name", name);
        
        NbtList itemsList = new NbtList();
        for (String item : items) {
            itemsList.add(NbtString.of(item));
        }
        nbt.put("items", itemsList);
    }
    
    @Override
    public void readFromNbt(NbtCompound nbt) {
        super.readFromNbt(nbt);
        this.name = nbt.getString("name");
        
        this.items.clear();
        NbtList itemsList = nbt.getList("items", NbtElement.STRING_TYPE);
        for (NbtElement element : itemsList) {
            items.add(element.asString());
        }
    }
    
    // ファクトリーメソッド
    public static AdvancedComponent create(String name) {
        AdvancedComponent component = new AdvancedComponent();
        component.setName(name);
        return component;
    }
    
    // ビジネスロジック
    public void addItem(String item) {
        if (!items.contains(item)) {
            items.add(item);
        }
    }
    
    public boolean removeItem(String item) {
        return items.remove(item);
    }
}
```

## 複合コンポーネント実装

### 3. LeverActionGunComponent パターン

#### インターフェース統合実装

```java
public class LeverActionGunComponent extends BaseItemComponent 
    implements FireTrigger, CyclingLever, Reloadable {
    
    // 状態管理フィールド
    private boolean leverDown = false;
    private boolean cycling = false;
    private boolean hammerReady = false;
    
    // タイミング制御
    private int fireCoolTime = 0;
    private int leverDownCoolTime = 0;
    private int leverUpCoolTime = 0;
    private int cycleCoolTime = 0;
    private int reloadCoolTime = 0;
    
    // コンポーネント連携
    private final Chamber chamber = new Chamber();
    private final MagazineComponent magazine = new MagazineComponent();
    
    // FireTrigger 実装
    @Override
    public boolean trigger(FireStartContext context) {
        if (!canTrigger()) return false;
        
        // 発射処理
        Optional<BulletComponent> bullet = chamber.shoot();
        if (bullet.isPresent()) {
            // 弾丸発射ロジック
            context.fireBullet(bullet.get());
            
            // サウンド再生
            context.getPlaySoundContext().playSound(
                LeverActionPlaySoundContext.Sound.FIRE);
            
            // アニメーション開始
            context.getAnimationContext().startAnimation("fire");
            
            // クールタイム設定
            fireCoolTime = getDataType().fireCoolLength();
            return true;
        } else {
            // 空撃ち
            context.getPlaySoundContext().playSound(
                LeverActionPlaySoundContext.Sound.DRY_FIRE);
            return false;
        }
    }
    
    @Override
    public boolean canTrigger() {
        return fireCoolTime <= 0 && hammerReady && !cycling;
    }
    
    // CyclingLever 実装
    @Override
    public boolean cycle(CycleTickContext context) {
        if (!canCycle()) return false;
        
        if (!leverDown) {
            // レバーダウン
            leverDown = true;
            leverDownCoolTime = getDataType().leverDownLength();
            
            // 薬莢排出
            if (chamber.hasSpentCartridge()) {
                Cartridge spent = chamber.ejectCartridge();
                context.ejectCartridge(spent);
            }
            
            context.getPlaySoundContext().playSound(
                LeverActionPlaySoundContext.Sound.CYCLE);
        } else {
            // レバーアップ
            leverDown = false;
            leverUpCoolTime = getDataType().leverUpLength();
            hammerReady = true;
            
            // 新しい弾丸装填
            magazine.getNextBullet().ifPresent(bullet -> {
                Cartridge cartridge = new Cartridge(bullet);
                chamber.loadCartridge(cartridge);
            });
        }
        
        cycling = true;
        cycleCoolTime = getDataType().cycleCoolLength();
        return true;
    }
    
    // Reloadable 実装
    @Override
    public boolean reload(ReloadStartContext context) {
        if (!canReload()) return false;
        
        // インベントリから弾薬を取得
        List<BulletComponent> availableBullets = 
            context.popBullets(magazine.getDataType().capacity(), 
                              magazine.getDataType().allowBullet());
        
        // マガジンに装填
        for (BulletComponent bullet : availableBullets) {
            if (!magazine.addBullet(bullet)) {
                // 入らなかった弾薬は返却
                context.returnBullets(List.of(bullet));
                break;
            }
        }
        
        context.getPlaySoundContext().playSound(
            LeverActionPlaySoundContext.Sound.RELOAD);
        
        reloadCoolTime = getDataType().reloadCoolLength();
        return true;
    }
    
    // 状態更新（tick処理）
    public void tick() {
        // クールタイム減少
        if (fireCoolTime > 0) fireCoolTime--;
        if (leverDownCoolTime > 0) leverDownCoolTime--;
        if (leverUpCoolTime > 0) leverUpCoolTime--;
        if (cycleCoolTime > 0) cycleCoolTime--;
        if (reloadCoolTime > 0) reloadCoolTime--;
        
        // サイクル完了チェック
        if (cycling && cycleCoolTime <= 0) {
            cycling = false;
        }
    }
    
    // データタイプ取得
    private LeverActionGunDataType getDataType() {
        return GunDataTypes.M1873; // 実際の実装では動的取得
    }
}
```

## コンポーネント連携パターン

### 4. Chamber-Cartridge-Bullet 階層

```java
// Chamber クラス：薬室の実装
public class Chamber {
    @Nullable
    private Cartridge currentCartridge = null;
    
    public boolean loadCartridge(Cartridge cartridge) {
        if (currentCartridge == null) {
            currentCartridge = cartridge;
            return true;
        }
        return false;
    }
    
    public Optional<BulletComponent> shoot() {
        if (currentCartridge != null && currentCartridge.canShoot()) {
            BulletComponent bullet = currentCartridge.getBullet().orElse(null);
            currentCartridge = new Cartridge(); // 空薬莢に変更
            return Optional.ofNullable(bullet);
        }
        return Optional.empty();
    }
    
    public Cartridge ejectCartridge() {
        Cartridge ejected = currentCartridge;
        currentCartridge = null;
        return ejected != null ? ejected : new Cartridge();
    }
    
    public boolean canShoot() {
        return currentCartridge != null && currentCartridge.canShoot();
    }
    
    public boolean hasSpentCartridge() {
        return currentCartridge != null && currentCartridge.isEmpty();
    }
}

// Cartridge クラス：薬莢の実装
public class Cartridge {
    @Nullable
    private BulletComponent bullet;
    
    public Cartridge() {
        this.bullet = null; // 空薬莢
    }
    
    public Cartridge(BulletComponent bullet) {
        this.bullet = bullet;
    }
    
    public boolean canShoot() {
        return bullet != null;
    }
    
    public boolean isEmpty() {
        return bullet == null;
    }
    
    public Optional<BulletComponent> getBullet() {
        return Optional.ofNullable(bullet);
    }
    
    // NBT永続化
    public void writeToNbt(NbtCompound nbt) {
        if (bullet != null) {
            NbtCompound bulletNbt = new NbtCompound();
            bullet.writeToNbt(bulletNbt);
            nbt.put("bullet", bulletNbt);
        }
    }
    
    public void readFromNbt(NbtCompound nbt) {
        if (nbt.contains("bullet")) {
            this.bullet = new BulletComponent();
            bullet.readFromNbt(nbt.getCompound("bullet"));
        } else {
            this.bullet = null;
        }
    }
}
```

## レジストリシステム活用

### 5. DataType と ComponentType の分離

```java
// DataType の定義
public record CustomGunDataType(
    int fireCoolLength,
    int reloadLength,
    float damage,
    String soundId
) {
    // バリデーション
    public CustomGunDataType {
        if (fireCoolLength < 0) throw new IllegalArgumentException("fireCoolLength must be >= 0");
        if (damage < 0) throw new IllegalArgumentException("damage must be >= 0");
    }
}

// ComponentType の定義
public class CustomGunComponentType implements ComponentType<CustomGunComponent> {
    private final CustomGunDataType dataType;
    
    public CustomGunComponentType(CustomGunDataType dataType) {
        this.dataType = dataType;
    }
    
    @Override
    public CustomGunComponent create() {
        return new CustomGunComponent(dataType);
    }
    
    @Override
    public Class<CustomGunComponent> getComponentClass() {
        return CustomGunComponent.class;
    }
}

// レジストリへの登録
public class CustomGunRegistration {
    public static final CustomGunDataType CUSTOM_GUN_DATA = 
        new CustomGunDataType(20, 40, 8.0f, "custom_gun_fire");
    
    public static final ComponentType<CustomGunComponent> CUSTOM_GUN_COMPONENT =
        new CustomGunComponentType(CUSTOM_GUN_DATA);
    
    public static void register() {
        GunDataTypes.register("custom_gun", CUSTOM_GUN_DATA);
        GunComponentTypes.register("custom_gun", CUSTOM_GUN_COMPONENT);
    }
}
```

## 実践的な使用例

### 6. ItemStack からのコンポーネント操作

```java
public class GunItemUsageExample {
    
    // コンポーネントの状態確認
    public static boolean canFireGun(ItemStack stack) {
        return LeverActionGunComponent.query(stack)
            .map(component -> component.canTrigger())
            .orElse(false);
    }
    
    // コンポーネントの状態変更
    public static ComponentResult fireGun(ItemStack stack, Player player) {
        return LeverActionGunComponent.execute(stack, component -> {
            FireStartContext context = new FireStartContext(player);
            if (component.trigger(context)) {
                return ComponentResult.MODIFIED; // 状態が変更された
            }
            return ComponentResult.NO_CHANGE; // 変更なし
        });
    }
    
    // 複数コンポーネントの連携操作
    public static ComponentResult reloadGun(ItemStack stack, Player player) {
        return LeverActionGunComponent.execute(stack, gunComponent -> {
            // 1. リロード可能性チェック
            if (!gunComponent.canReload()) {
                return ComponentResult.NO_CHANGE;
            }
            
            // 2. UniqueComponent でアイテム識別
            UUID itemId = UniqueComponent.getOrSet(stack);
            
            // 3. リロード実行
            ReloadStartContext context = new ReloadStartContext(player);
            boolean reloaded = gunComponent.reload(context);
            
            // 4. HUD更新通知
            if (reloaded) {
                ServerHudManager.getInstance().updateHud(
                    player, itemId, LeverActionHudState.of(gunComponent));
            }
            
            return reloaded ? ComponentResult.MODIFIED : ComponentResult.NO_CHANGE;
        });
    }
    
    // 安全なコンポーネントアクセス
    public static Optional<String> getGunStatus(ItemStack stack) {
        return LeverActionGunComponent.query(stack)
            .map(component -> {
                int bullets = component.getMagazine().getBulletCount();
                boolean chambered = component.getChamber().canShoot();
                boolean canFire = component.canTrigger();
                
                return String.format("Bullets: %d, Chambered: %s, Ready: %s", 
                    bullets, chambered, canFire);
            });
    }
}
```

## NBT永続化のベストプラクティス

### 7. 効率的なNBT管理

```java
public class OptimizedComponent extends BaseItemComponent {
    private static final String NBT_VERSION = "version";
    private static final String NBT_DATA = "data";
    private static final int CURRENT_VERSION = 2;
    
    private Map<String, Object> data = new HashMap<>();
    
    @Override
    public void writeToNbt(NbtCompound nbt) {
        super.writeToNbt(nbt);
        
        // バージョン情報の保存
        nbt.putInt(NBT_VERSION, CURRENT_VERSION);
        
        // データの効率的な保存
        NbtCompound dataNbt = new NbtCompound();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            writeValueToNbt(dataNbt, entry.getKey(), entry.getValue());
        }
        nbt.put(NBT_DATA, dataNbt);
    }
    
    @Override
    public void readFromNbt(NbtCompound nbt) {
        super.readFromNbt(nbt);
        
        int version = nbt.getInt(NBT_VERSION);
        
        // バージョン互換性処理
        if (version < CURRENT_VERSION) {
            migrateFromOldVersion(nbt, version);
        }
        
        // データの読み込み
        if (nbt.contains(NBT_DATA)) {
            NbtCompound dataNbt = nbt.getCompound(NBT_DATA);
            for (String key : dataNbt.getKeys()) {
                data.put(key, readValueFromNbt(dataNbt, key));
            }
        }
    }
    
    private void migrateFromOldVersion(NbtCompound nbt, int oldVersion) {
        // 古いバージョンからのマイグレーション処理
        switch (oldVersion) {
            case 1 -> migrateFromV1(nbt);
            // 他のバージョン...
        }
    }
}
```

## まとめ

ActionArmsのコンポーネントシステムは、以下の設計原則に基づいています：

### 🔧 設計原則
1. **単一責任**: 各コンポーネントは明確な責任を持つ
2. **インターフェース分離**: 機能別インターフェースで疎結合
3. **不変性**: 可能な限り不変オブジェクトを使用
4. **型安全**: ジェネリクスと Optional で安全性確保

### 📊 実装パターン
1. **execute/query/update**: 状態変更の明確な分離
2. **ComponentResult**: 変更有無の明示的な表現
3. **静的メソッド**: ItemStack 操作の標準化
4. **NBT永続化**: バージョン管理と互換性

### 🚀 パフォーマンス
1. **メモリ効率**: 必要時のみコンポーネント作成
2. **型安全キャッシング**: レジストリによる最適化
3. **バッチ処理**: 複数操作の効率的実行

この実装パターンにより、拡張性と保守性を両立した堅牢なシステムを構築できます。

---

**作成日**: 2025-06-19  
**情報源**: 知識の泉（item.json）  
**関連システム**: レジストリ、NBT永続化、ネットワーク同期  
**適用ガイドライン**: documentation-creation-guide.md
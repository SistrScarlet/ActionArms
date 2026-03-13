package net.sistr.actionarms.item.component;

import net.minecraft.nbt.NbtCompound;
import net.sistr.actionarms.item.data.BulletData;
import net.sistr.actionarms.item.data.SAAGunData;
import net.sistr.actionarms.item.util.AnimationContext;
import net.sistr.actionarms.item.util.Cylinder;

public class SAAGunComponent implements IComponent {

    public enum Phase {
        IDLE,
        COCKING,
        FULL_COCK,
        FIRE_COOLING,
        GATE_OPEN,
        EJECTING,
        LOADING
    }

    @FunctionalInterface
    public interface SoundContext {
        void playSound(String sound);
    }

    @FunctionalInterface
    public interface FireContext {
        void fire(SAAGunComponent gun, BulletData bullet);
    }

    private final SAAGunData gunData;
    private final Cylinder cylinder;
    private Phase phase = Phase.IDLE;
    private float phaseTimer;
    private float cooldownTime;
    private boolean hammerCocked;

    public SAAGunComponent(SAAGunData gunData) {
        this.gunData = gunData;
        this.cylinder = new Cylinder(gunData.cylinderCapacity());
    }

    // === tick ===

    public boolean tick(SoundContext soundContext, float timeDelta, boolean active) {
        boolean changed = false;

        if (this.cooldownTime > 0) {
            this.cooldownTime = Math.max(0, this.cooldownTime - timeDelta);
            changed = true;
        }

        if (!active) return changed;

        if (this.phaseTimer > 0) {
            this.phaseTimer = Math.max(0, this.phaseTimer - timeDelta);
            changed = true;
            if (this.phaseTimer <= 0) {
                onPhaseComplete(soundContext);
            }
        }

        return changed;
    }

    private void onPhaseComplete(SoundContext soundContext) {
        switch (this.phase) {
            case COCKING:
                this.hammerCocked = true;
                this.phase = Phase.FULL_COCK;
                break;
            case FIRE_COOLING:
                this.phase = Phase.IDLE;
                break;
            case EJECTING:
                this.phase = Phase.GATE_OPEN;
                smartGateRotate();
                break;
            case LOADING:
                this.phase = Phase.GATE_OPEN;
                smartGateRotate();
                if (this.cylinder.isAllReady()) {
                    closeGate();
                    soundContext.playSound("GATE_CLOSE");
                }
                break;
            default:
                break;
        }
    }

    // === コック（ゲート閉じ） ===

    public boolean canCockHammer() {
        return !this.hammerCocked
                && !this.isGateOpen()
                && this.phase != Phase.COCKING
                && this.cooldownTime <= 0;
    }

    public boolean cockHammer(SoundContext soundContext, AnimationContext animContext) {
        if (!canCockHammer()) return false;
        this.cylinder.cockRotate();
        this.phase = Phase.COCKING;
        this.phaseTimer = this.gunData.cockLength();
        soundContext.playSound("COCK");
        animContext.setAnimation("cock", 0);
        return true;
    }

    // === 射撃 ===

    public boolean canPullTrigger() {
        return this.hammerCocked && !this.isGateOpen() && this.cooldownTime <= 0;
    }

    public boolean pullTrigger(
            SoundContext soundContext, AnimationContext animContext, FireContext fireContext) {
        if (!canPullTrigger()) return false;

        this.hammerCocked = false;

        if (this.cylinder.canShootFiring()) {
            var bullet = this.cylinder.shootFiring().orElseThrow();
            fireContext.fire(this, bullet);
            soundContext.playSound("FIRE");
            animContext.setAnimation("fire", 0);
        } else {
            soundContext.playSound("DRY_FIRE");
            animContext.setAnimation("dry_fire", 0);
        }

        this.phase = Phase.FIRE_COOLING;
        this.phaseTimer = 0;
        this.cooldownTime = this.gunData.fireCoolLength();
        return true;
    }

    // === ゲート開閉 ===

    public boolean isGateOpen() {
        return this.phase == Phase.GATE_OPEN
                || this.phase == Phase.EJECTING
                || this.phase == Phase.LOADING;
    }

    public void openGate() {
        this.hammerCocked = false;
        this.phase = Phase.GATE_OPEN;
        this.phaseTimer = 0;
        smartGateRotate();
    }

    public void closeGate() {
        alignCylinderForFiring();
        this.phase = Phase.IDLE;
        this.phaseTimer = 0;
    }

    /**
     * ゲート閉鎖時のシリンダー位置合わせ。
     *
     * <p>コック→射撃を繰り返したとき、!canShoot な薬室が最後に来るように配置する。 firingIndex に !canShoot、その CW (firingIndex-1) に
     * canShoot が来る位置を探す。 見つからなければ回転しない。
     */
    private void alignCylinderForFiring() {
        int capacity = this.cylinder.getCapacity();

        for (int i = 0; i < capacity; i++) {
            int cw = (i - 1 + capacity) % capacity;
            if (!this.cylinder.getChamberAt(i).canShoot()
                    && this.cylinder.getChamberAt(cw).canShoot()) {
                // この位置に firingIndex を合わせる
                while (this.cylinder.getFiringIndex() != i) {
                    this.cylinder.cockRotate();
                }
                return;
            }
        }
        // 全て canShoot or 全て !canShoot → 回転不要
    }

    /**
     * ゲート開放中のスマート回転。以下の優先順で回転方向を決定する:
     *
     * <ol>
     *   <li>左（loadRotate方向）に空薬莢 → 左回転
     *   <li>ゲート位置自体に空薬莢 → 回転なし
     *   <li>右（cockRotate方向）に空薬莢 → 右回転
     *   <li>左に空薬室 → 左回転
     *   <li>ゲート位置自体が空 → 回転なし
     *   <li>右に空薬室 → 右回転
     *   <li>いずれも該当しない → 回転なし
     * </ol>
     */
    public void smartGateRotate() {
        int capacity = this.cylinder.getCapacity();
        int gate = this.cylinder.gateIndex();
        // cockRotate 方向（時計回り、firingIndex--）
        int cw = (gate - 1 + capacity) % capacity;
        // loadRotate 方向（反時計回り、firingIndex++）
        int ccw = (gate + 1) % capacity;

        // 空薬莢を優先（cw → ゲート → ccw）
        if (this.cylinder.getChamberAt(cw).shouldEject()) {
            this.cylinder.cockRotate();
        } else if (this.cylinder.getChamberAt(gate).shouldEject()) {
            // ゲート位置に既に空薬莢 → 回転不要
        } else if (this.cylinder.getChamberAt(ccw).shouldEject()) {
            this.cylinder.loadRotate();
        }
        // 空薬莢がなければ空薬室を探す
        else if (this.cylinder.getChamberAt(gate).isEmpty()) {
            if (!this.cylinder.getChamberAt(ccw).isEmpty()) {
                // ゲートが空で ccw が空でない → 回転不要（ゲートに装填可）
            } else if (!this.cylinder.getChamberAt(cw).isEmpty()) {
                // ccw も空 → ccw 方向に回転
                this.cylinder.loadRotate();
            } else {
                // 全て空 → デフォルト cw 回転
                this.cylinder.cockRotate();
            }
        } else if (this.cylinder.getChamberAt(ccw).isEmpty()) {
            this.cylinder.loadRotate();
        } else if (this.cylinder.getChamberAt(cw).isEmpty()) {
            this.cylinder.cockRotate();
        }
        // いずれも該当しない → デフォルト ccw 回転
        else {
            this.cylinder.loadRotate();
        }
    }

    // === 排莢（ゲート開放中 + COCK キー） ===

    public boolean canEjectAtGate() {
        return this.phase == Phase.GATE_OPEN;
    }

    public boolean ejectAtGate(SoundContext soundContext, AnimationContext animContext) {
        if (!canEjectAtGate()) return false;
        this.cylinder.ejectAtGate();
        this.phase = Phase.EJECTING;
        this.phaseTimer = this.gunData.ejectLength();
        soundContext.playSound("EJECT");
        animContext.setAnimation("eject", 0);
        return true;
    }

    // === 装填（ゲート開放中 + RELOAD キー） ===

    public boolean canLoadAtGate() {
        return this.phase == Phase.GATE_OPEN && this.cylinder.gateChamber().isEmpty();
    }

    public boolean loadAtGate(BulletData bullet) {
        if (!canLoadAtGate()) return false;
        this.cylinder.loadAtGate(bullet);
        this.phase = Phase.LOADING;
        this.phaseTimer = this.gunData.loadLength();
        return true;
    }

    // === IComponent (NBT永続化) ===

    @Override
    public void read(NbtCompound nbt) {
        this.hammerCocked = nbt.getBoolean("hammerCocked");
        this.phase = readPhase(nbt.getString("phase"));
        this.phaseTimer = nbt.getFloat("phaseTimer");
        this.cooldownTime = nbt.getFloat("cooldownTime");
        this.cylinder.read(nbt.getCompound("cylinder"));
    }

    @Override
    public void write(NbtCompound nbt) {
        nbt.putBoolean("hammerCocked", this.hammerCocked);
        nbt.putString("phase", this.phase.name());
        nbt.putFloat("phaseTimer", this.phaseTimer);
        nbt.putFloat("cooldownTime", this.cooldownTime);
        var cylinderNbt = new NbtCompound();
        this.cylinder.write(cylinderNbt);
        nbt.put("cylinder", cylinderNbt);
    }

    private static Phase readPhase(String name) {
        try {
            return Phase.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Phase.IDLE;
        }
    }

    // === getter ===

    public boolean isHammerCocked() {
        return this.hammerCocked;
    }

    public Cylinder getCylinder() {
        return this.cylinder;
    }

    public Phase getPhase() {
        return this.phase;
    }

    public SAAGunData getGunData() {
        return this.gunData;
    }
}

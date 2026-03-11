package net.sistr.actionarms.item.component;

import net.minecraft.nbt.NbtCompound;
import net.sistr.actionarms.item.data.LeverActionGunData;
import net.sistr.actionarms.item.data.MagazineData;
import net.sistr.actionarms.item.util.*;

public class LeverActionGunComponent implements IComponent, FireTrigger, CyclingLever, Reloadable {
    private final LeverActionGunData gunData;
    private final Chamber chamber = new Chamber(null);
    private final MagazineComponent magazine;
    private boolean hammerReady;
    private GunPhase phase = GunPhase.IDLE;
    private float phaseTimer;
    private float cooldownTime;

    public enum GunPhase {
        IDLE,
        CYCLE_DOWN,
        CYCLE_UP,
        RELOADING,
        COOLING;

        public boolean isCycling() {
            return this == CYCLE_DOWN || this == CYCLE_UP;
        }

        public boolean isReloading() {
            return this == RELOADING;
        }
    }

    public LeverActionGunComponent(LeverActionGunData gunData, MagazineData magazineData) {
        this.gunData = gunData;
        this.magazine = new MagazineComponent(magazineData);
    }

    // tick処理
    public boolean tick(
            LeverActionPlaySoundContext playSoundContext,
            CycleTickContext cycleContext,
            ReloadTickContext reloadContext,
            float timeDelta,
            boolean active) {
        boolean markDuty = false;

        if (this.cooldownTime > 0) {
            this.cooldownTime = Math.max(0, this.cooldownTime - timeDelta);
            markDuty = true;
            if (this.cooldownTime == 0 && this.phase == GunPhase.COOLING) {
                this.phase = GunPhase.IDLE;
            }
        }

        if (active) {
            switch (this.phase) {
                case CYCLE_DOWN:
                    cycleTick(playSoundContext, cycleContext, timeDelta, false);
                    markDuty = true;
                    break;
                case CYCLE_UP:
                    cycleTick(playSoundContext, cycleContext, timeDelta, true);
                    markDuty = true;
                    break;
                case RELOADING:
                    reloadTick(playSoundContext, reloadContext, timeDelta);
                    markDuty = true;
                    break;
                default:
                    break;
            }
        }
        return markDuty;
    }

    // FireTriggerインターフェース実装
    @Override
    public boolean pullTrigger(
            LeverActionPlaySoundContext playSoundContext,
            AnimationContext animationContext,
            FireStartContext fireContext) {
        if (!canPullTrigger()) {
            return false;
        }
        if (!canShoot()) {
            playSoundContext.playSound(LeverActionPlaySoundContext.Sound.DRY_FIRE);
            animationContext.setAnimation("dry_fire", 0);
            this.hammerReady = false;
            return true;
        }
        var bullet = this.chamber.shoot().orElseThrow();
        fireContext.fire(this, bullet);
        playSoundContext.playSound(LeverActionPlaySoundContext.Sound.FIRE);
        animationContext.setAnimation("fire", 0);
        this.hammerReady = false;
        this.phase = GunPhase.COOLING;
        this.phaseTimer = 0;
        this.cooldownTime = this.gunData.fireCoolLength();
        return true;
    }

    @Override
    public boolean canPullTrigger() {
        if (!this.hammerReady || this.cooldownTime > 0) {
            return false;
        }
        return switch (this.phase) {
            case IDLE, COOLING -> true;
            case CYCLE_DOWN -> this.phaseTimer <= this.gunData.cycleCancelableLength();
            case CYCLE_UP -> false;
            case RELOADING -> this.phaseTimer <= this.gunData.reloadCancelableLength();
        };
    }

    // CyclingLeverインターフェース実装
    private void cycleTick(
            LeverActionPlaySoundContext playSoundContext,
            CycleTickContext context,
            float timeDelta,
            boolean leverDown) {
        this.phaseTimer = Math.max(0, this.phaseTimer - timeDelta);
        if (this.phaseTimer == 0) {
            if (leverDown) {
                // サイクル終了処理（レバーが上がる）
                this.phase = GunPhase.COOLING;
                this.cooldownTime = this.gunData.cycleCoolLength();
                this.hammerReady = true;

                // チューブマガジンはFILO
                this.magazine
                        .popFirstBullet()
                        .ifPresent(bullet -> this.chamber.loadCartridge(new Cartridge(bullet)));
            } else {
                // サイクル折り返し処理（レバーが下がる）
                this.phase = GunPhase.CYCLE_UP;
                this.phaseTimer = this.gunData.leverUpLength();
                this.hammerReady = false;
                this.chamber.ejectCartridge().ifPresent(context::ejectCartridge);
            }
        }
    }

    @Override
    public boolean cycleLever(
            LeverActionPlaySoundContext playSoundContext, AnimationContext animationContext) {
        if (!canCycleLever()) {
            return false;
        }
        boolean leverDown = isLeverDown();
        if (this.chamber.isInCartridge()) {
            animationContext.setAnimation("cycle", leverDown ? this.gunData.leverDownLength() : 0);
        } else {
            animationContext.setAnimation(
                    "cycle_empty", leverDown ? this.gunData.leverDownLength() : 0);
        }
        playSoundContext.playSound(LeverActionPlaySoundContext.Sound.CYCLE);
        if (leverDown) {
            this.phase = GunPhase.CYCLE_UP;
            this.phaseTimer = this.gunData.leverUpLength();
        } else {
            this.phase = GunPhase.CYCLE_DOWN;
            this.phaseTimer = this.gunData.leverDownLength();
        }
        return true;
    }

    @Override
    public boolean canCycleLever() {
        if (this.cooldownTime > 0) {
            return false;
        }
        return switch (this.phase) {
            case IDLE -> true;
            case COOLING -> true;
            case CYCLE_DOWN, CYCLE_UP -> false;
            case RELOADING -> this.phaseTimer <= this.gunData.reloadCancelableLength();
        };
    }

    @Override
    public boolean shouldCycleLever() {
        return (this.chamber.canShoot() && (isLeverDown() || !hammerReady))
                || (!this.chamber.canShoot() && this.magazine.hasBullet());
    }

    @Override
    public boolean isHammerReady() {
        return this.hammerReady;
    }

    @Override
    public boolean isLeverDown() {
        return this.phase == GunPhase.CYCLE_UP;
    }

    // Reloadableインターフェース実装
    private void reloadTick(
            LeverActionPlaySoundContext playSoundContext,
            ReloadTickContext context,
            float timeDelta) {
        this.phaseTimer = Math.max(0, this.phaseTimer - timeDelta);
        if (this.phaseTimer == 0) {
            this.phase = GunPhase.COOLING;
            this.cooldownTime = this.gunData.reloadCoolLength();
            if (this.magazine.canAddBullet()) {
                var bullets =
                        context.popBullets(
                                this.magazine.getMagazineType().allowBullet(),
                                this.gunData.reloadCount());
                if (bullets.isEmpty()) {
                    return;
                }
                // チューブマガジンはFILO
                var nonLoaded = this.magazine.addFirstBullets(bullets, true);
                context.returnBullets(nonLoaded);
            }
        }
    }

    @Override
    public boolean loadBullet(
            LeverActionPlaySoundContext playSoundContext,
            AnimationContext animationContext,
            ReloadStartContext context) {
        if (!canLoadBullet(context)) {
            return false;
        }
        int maxBullets = this.magazine.getMaxCapacity();
        int bullets = this.magazine.getBullets().size();
        if (bullets + this.gunData.reloadCount() >= maxBullets) {
            animationContext.setAnimation("reload_end", 0);
        } else {
            animationContext.setAnimation("reload", 0);
        }
        playSoundContext.playSound(LeverActionPlaySoundContext.Sound.RELOAD);
        this.phase = GunPhase.RELOADING;
        this.phaseTimer = this.gunData.reloadLength();
        return true;
    }

    @Override
    public boolean canLoadBullet(ReloadStartContext context) {
        if (this.cooldownTime > 0 || this.phase.isCycling()) {
            return false;
        }
        return !this.phase.isReloading()
                && this.magazine.canAddBullet()
                && context.hasBullet(this.magazine.getMagazineType().allowBullet());
    }

    @Override
    public boolean shouldLoadBullet() {
        return this.magazine.canAddBullet();
    }

    private boolean canShoot() {
        return canPullTrigger() && this.chamber.canShoot();
    }

    // IItemComponentインターフェース実装（NBT永続化）
    @Override
    public void read(NbtCompound nbt) {
        this.hammerReady = nbt.getBoolean("hammerReady");
        this.phase = readPhase(nbt.getString("phase"));
        this.phaseTimer = nbt.getFloat("phaseTimer");
        this.cooldownTime = nbt.getFloat("cooldownTime");
        this.chamber.read(nbt.getCompound("chamber"));
        this.magazine.read(nbt.getCompound("magazine"));
    }

    @Override
    public void write(NbtCompound nbt) {
        nbt.putBoolean("hammerReady", this.hammerReady);
        nbt.putString("phase", this.phase.name());
        nbt.putFloat("phaseTimer", this.phaseTimer);
        nbt.putFloat("cooldownTime", this.cooldownTime);
        var chamberNbt = new NbtCompound();
        this.chamber.write(chamberNbt);
        nbt.put("chamber", chamberNbt);
        var magazineNbt = new NbtCompound();
        this.magazine.write(magazineNbt);
        nbt.put("magazine", magazineNbt);
    }

    private static GunPhase readPhase(String name) {
        try {
            return GunPhase.valueOf(name);
        } catch (IllegalArgumentException e) {
            return GunPhase.IDLE;
        }
    }

    // getter

    public LeverActionGunData getGunData() {
        return gunData;
    }

    public Chamber getChamber() {
        return chamber;
    }

    public MagazineComponent getMagazine() {
        return magazine;
    }

    public GunPhase getPhase() {
        return phase;
    }

    public boolean isCycling() {
        return this.phase.isCycling();
    }

    public boolean isReloading() {
        return this.phase.isReloading();
    }

    // テスト用
    void setHammerReady(boolean hammerReady) {
        this.hammerReady = hammerReady;
    }
}

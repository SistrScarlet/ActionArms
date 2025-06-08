package net.sistr.actionarms.item.component;

import net.minecraft.nbt.NbtCompound;

public class LeverActionGunComponent implements IItemComponent, FireTrigger, CyclingLever, Reloadable {
    private final LeverActionGunDataType gunType;
    private final Chamber chamber = new Chamber(null);
    private final MagazineComponent magazine;
    private boolean hammerReady;
    private boolean leverDown;
    private boolean cycling;
    private boolean reloading;
    private float cycleTime;
    private float reloadTime;
    private float fireCoolTime;
    private float cycleCoolTime;
    private float reloadCoolTime;
    private float cycleCancelableTime;
    private float reloadCancelableTime;

    public LeverActionGunComponent(LeverActionGunDataType gunType, MagazineComponent magazine) {
        this.gunType = gunType;
        this.magazine = magazine;
    }

    // tick処理
    public void tick(CycleTickContext cycleContext, ReloadTickContext reloadContext, float timeDelta, boolean active) {
        // 時間経過処理
        this.fireCoolTime = Math.max(0, this.fireCoolTime - timeDelta);
        this.cycleCoolTime = Math.max(0, this.cycleCoolTime - timeDelta);
        this.reloadCoolTime = Math.max(0, this.reloadCoolTime - timeDelta);

        // アクティブ時処理
        if (active) {
            // サイクル処理
            if (this.cycling) {
                cycleTick(cycleContext, timeDelta);
            }

            // リロード処理
            if (this.reloading) {
                reloadTick(reloadContext, timeDelta);
            }
        }
    }

    // FireTriggerインターフェース実装
    @Override
    public boolean trigger(AnimationContext animationContext, FireStartContext context) {
        if (!canShoot()) {
            // todo:空撃ち音の再生処理
            return false;
        }
        var bullet = this.chamber.shoot().orElseThrow();
        context.fire(bullet);
        animationContext.setAnimation("fire", 0);
        this.hammerReady = false;
        this.fireCoolTime = this.gunType.fireCoolLength();
        return true;
    }

    @Override
    public boolean canTrigger() {
        return canShoot();
    }

    // CyclingLeverインターフェース実装
    private void cycleTick(CycleTickContext context, float timeDelta) {
        this.cycleTime = Math.max(0, this.cycleTime - timeDelta);
        this.cycleCancelableTime = Math.max(0, this.cycleCancelableTime - timeDelta);
        // サイクルが節目を迎えたとき
        if (this.cycleTime == 0) {
            if (this.leverDown) {
                // サイクル終了処理
                this.leverDown = false;
                this.cycling = false;
                this.cycleCoolTime = this.gunType.cycleCoolLength();
                this.hammerReady = true;

                // チューブマガジンはFILO
                this.magazine.popFirstBullet()
                        .ifPresent(bullet -> {
                            this.chamber.setCartridge(new Cartridge(bullet));
                        });
            } else {
                // サイクル折り返し処理
                this.leverDown = true;
                this.cycleTime = this.gunType.leverDownLength();
                this.hammerReady = false;
                this.chamber.ejectCartridge()
                        .ifPresent(context::ejectCartridge);
            }
        }
    }

    @Override
    public boolean cycle(AnimationContext context) {
        if (!canCycle()) {
            return false;
        }
        context.setAnimation("cycle", this.leverDown ? this.gunType.leverDownLength() : 0);
        this.cycling = true;
        this.reloading = false;
        if (this.leverDown) {
            this.cycleTime = this.gunType.leverUpLength();
        } else {
            this.cycleTime = this.gunType.leverDownLength();
        }
        this.cycleCancelableTime = this.gunType.cycleCancelableLength();
        return true;
    }

    @Override
    public boolean canCycle() {
        return !this.cycling
                && (!this.reloading || this.reloadCancelableTime > 0);
    }

    @Override
    public boolean shouldCycle() {
        return canCycle()
                && ((this.chamber.canShoot() && leverDown)
                || (!this.chamber.canShoot() && this.magazine.hasBullet()));
    }

    @Override
    public boolean isHammerReady() {
        return this.hammerReady;
    }

    @Override
    public boolean isLeverDown() {
        return this.leverDown;
    }

    // Reloadableインターフェース実装
    private void reloadTick(ReloadTickContext context, float timeDelta) {
        this.reloadTime = Math.max(0, this.reloadTime - timeDelta);
        this.reloadCancelableTime = Math.max(0, this.reloadCancelableTime - timeDelta);
        if (this.reloadTime == 0) {
            this.reloading = false;
            this.reloadCoolTime = this.gunType.reloadCoolLength();
            if (this.magazine.canAddBullet()) {
                var bullets = context.popBullets(
                        this.magazine.getMagazineType().allowBullet(),
                        this.gunType.reloadCount()
                );
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
    public boolean reload(AnimationContext animationContext) {
        if (!canReload()) {
            return false;
        }
        animationContext.setAnimation("reload", 0);
        this.reloading = true;
        this.cycling = false;
        this.reloadTime = this.gunType.reloadLength();
        this.reloadCancelableTime = this.gunType.reloadCancelableLength();
        return true;
    }

    @Override
    public boolean canReload() {
        return !this.reloading
                && (!this.cycling || this.cycleCancelableTime > 0)
                && this.magazine.canAddBullet();
    }

    @Override
    public boolean shouldReload() {
        return canReload() && !this.chamber.canShoot() && this.magazine.isEmpty();
    }

    // 内部ヘルパーメソッド
    private boolean canShoot() {
        return this.hammerReady
                && !this.leverDown
                && this.fireCoolTime == 0
                && this.chamber.canShoot();
    }

    // IItemComponentインターフェース実装（NBT永続化）
    @Override
    public void read(NbtCompound nbt) {
        this.cycling = nbt.getBoolean("cycling");
        this.reloading = nbt.getBoolean("reloading");
        this.hammerReady = nbt.getBoolean("hammerReady");
        this.leverDown = nbt.getBoolean("leverDown");
        this.cycleTime = nbt.getFloat("cycleTime");
        this.reloadCoolTime = nbt.getFloat("reloadCoolTime");
        this.fireCoolTime = nbt.getFloat("fireCoolTime");
        this.cycleCancelableTime = nbt.getFloat("cycleCancelableTime");
        this.reloadCancelableTime = nbt.getFloat("reloadCancelableTime");
        this.cycleCoolTime = nbt.getFloat("cycleCoolTime");
        this.reloadTime = nbt.getFloat("reloadTime");
        this.chamber.read(nbt.getCompound("chamber"));
        this.magazine.read(nbt.getCompound("magazine"));
    }

    @Override
    public void write(NbtCompound nbt) {
        nbt.putBoolean("cycling", this.cycling);
        nbt.putBoolean("reloading", this.reloading);
        nbt.putBoolean("hammerReady", this.hammerReady);
        nbt.putBoolean("leverDown", this.leverDown);
        nbt.putFloat("cycleTime", this.cycleTime);
        nbt.putFloat("reloadCoolTime", this.reloadCoolTime);
        nbt.putFloat("fireCoolTime", this.fireCoolTime);
        nbt.putFloat("cycleCancelableTime", this.cycleCancelableTime);
        nbt.putFloat("reloadCancelableTime", this.reloadCancelableTime);
        nbt.putFloat("cycleCoolTime", this.cycleCoolTime);
        nbt.putFloat("reloadTime", this.reloadTime);
        var chamberNbt = new NbtCompound();
        this.chamber.write(chamberNbt);
        nbt.put("chamber", chamberNbt);
        var magazineNbt = new NbtCompound();
        this.magazine.write(magazineNbt);
        nbt.put("magazine", magazineNbt);
    }
}

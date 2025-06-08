package net.sistr.actionarms.component;

import net.minecraft.nbt.NbtCompound;

public class LeverActionGun implements FireTrigger, CyclingLever, Reloadable {
    private final LeverActionGunType type;
    private final Chamber chamber = new Chamber(null);
    private final Magazine magazine;
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

    public LeverActionGun(LeverActionGunType type, MagazineType magazineType) {
        this.type = type;
        this.magazine = new Magazine(magazineType);
    }

    public void tick(boolean active, CycleTickContext cycleContext, ReloadTickContext reloadContext, float timeDelta) {
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

    @Override
    public boolean trigger(FireStartContext context) {
        if (!canShoot()) {
            // todo:空撃ち音の再生処理
            return false;
        }
        var bullet = this.chamber.shoot().orElseThrow();
        context.fire(bullet);
        this.hammerReady = false;
        this.fireCoolTime = this.type.fireCoolLength();
        return true;
    }

    @Override
    public boolean canTrigger() {
        return canShoot();
    }

    @Override
    public boolean isFiring() {
        return this.fireCoolTime > 0;
    }

    @Override
    public float getFiringTime() {
        return this.type.fireCoolLength() - this.fireCoolTime;
    }

    private void cycleTick(CycleTickContext context, float timeDelta) {
        this.cycleTime = Math.max(0, this.cycleTime - timeDelta);
        this.cycleCancelableTime = Math.max(0, this.cycleCancelableTime - timeDelta);
        // サイクルが節目を迎えたとき
        if (this.cycleTime == 0) {
            if (this.leverDown) {
                // サイクル終了処理
                this.leverDown = false;
                this.cycling = false;
                this.cycleCoolTime = this.type.cycleCoolLength();
                this.hammerReady = true;

                // チューブマガジンはFILO
                this.magazine.popFirstBullet()
                        .ifPresent(bullet -> {
                            this.chamber.setCartridge(new Cartridge(bullet));
                        });
            } else {
                // サイクル折り返し処理
                this.leverDown = true;
                this.cycleTime = this.type.leverDownLength();
                this.hammerReady = false;
                this.chamber.ejectCartridge()
                        .ifPresent(context::ejectCartridge);
            }
        }
    }

    @Override
    public boolean cycle(CycleStartContext context) {
        if (!canCycle()) {
            return false;
        }
        context.cycle(this.leverDown ? this.type.leverDownLength() : 0);
        this.cycling = true;
        this.reloading = false;
        if (this.leverDown) {
            this.cycleTime = this.type.leverUpLength();
        } else {
            this.cycleTime = this.type.leverDownLength();
        }
        this.cycleCancelableTime = this.type.cycleCancelableLength();
        return true;
    }

    @Override
    public boolean canCycle() {
        return !this.cycling
                && (!this.reloading || this.reloadCancelableTime > 0);
    }

    @Override
    public boolean shouldCycle() {
        return this.chamber.getCartridge().isEmpty()
                || !this.chamber.getCartridge().get().canShoot();
    }

    @Override
    public boolean isHammerReady() {
        return this.hammerReady;
    }

    @Override
    public boolean isLeverDown() {
        return this.leverDown;
    }

    @Override
    public boolean isCycling() {
        return this.cycling;
    }

    @Override
    public float getCyclingTime() {
        if (this.leverDown) {
            return this.type.leverUpLength() + this.type.leverDownLength() - this.cycleTime;
        }
        return this.type.leverDownLength() - this.cycleTime;
    }

    private void reloadTick(ReloadTickContext reloadContext, float timeDelta) {
        this.reloadTime = Math.max(0, this.reloadTime - timeDelta);
        this.reloadCancelableTime = Math.max(0, this.reloadCancelableTime - timeDelta);
        if (this.reloadTime == 0) {
            this.reloading = false;
            this.reloadCoolTime = this.type.reloadCoolLength();
            if (this.magazine.canAddBullet()) {
                var bullets = reloadContext.popBullets(
                        this.magazine.getType().allowBullet(),
                        this.type.reloadCount()
                );
                if (bullets.isEmpty()) {
                    return;
                }
                // チューブマガジンはFILO
                var nonLoaded = this.magazine.addFirstBullets(bullets, true);
                reloadContext.returnBullets(nonLoaded);
            }
        }
    }

    @Override
    public boolean reload(ReloadStartContext context) {
        if (!canReload()) {
            return false;
        }
        context.reload(0);
        this.reloading = true;
        this.cycling = false;
        this.reloadTime = this.type.reloadLength();
        this.reloadCancelableTime = this.type.reloadCancelableLength();
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
        return this.magazine.isEmpty();
    }

    @Override
    public boolean isReloading() {
        return this.reloading;
    }

    @Override
    public float getReloadingTime() {
        return this.type.reloadLength() - this.reloadTime;
    }

    private boolean canShoot() {
        return this.hammerReady
                && !this.leverDown
                && this.fireCoolTime == 0
                && this.chamber.canShoot();
    }

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

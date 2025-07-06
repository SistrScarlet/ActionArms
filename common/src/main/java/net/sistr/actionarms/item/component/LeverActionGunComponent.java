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

    public LeverActionGunComponent(LeverActionGunDataType gunType, MagazineComponent magazine) {
        this.gunType = gunType;
        this.magazine = magazine;
    }

    // tick処理
    public boolean tick(LeverActionPlaySoundContext playSoundContext, CycleTickContext cycleContext,
                        ReloadTickContext reloadContext, float timeDelta, boolean active) {
        boolean markDuty = false;

        // 時間経過処理
        if (this.fireCoolTime > 0) {
            this.fireCoolTime = Math.max(0, this.fireCoolTime - timeDelta);
            markDuty = true;
        }
        if (cycleCoolTime > 0) {
            this.cycleCoolTime = Math.max(0, this.cycleCoolTime - timeDelta);
            markDuty = true;
        }
        if (reloadCoolTime > 0) {
            this.reloadCoolTime = Math.max(0, this.reloadCoolTime - timeDelta);
            markDuty = true;
        }


        // アクティブ時処理
        if (active) {
            // サイクル処理
            if (this.cycling) {
                cycleTick(playSoundContext, cycleContext, timeDelta);
                markDuty = true;
            }

            // リロード処理
            if (this.reloading) {
                reloadTick(playSoundContext, reloadContext, timeDelta);
                markDuty = true;
            }
        }
        return markDuty;
    }

    // FireTriggerインターフェース実装
    @Override
    public boolean trigger(LeverActionPlaySoundContext playSoundContext, AnimationContext animationContext,
                           FireStartContext fireContext) {
        if (!canTrigger()) {
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
        this.fireCoolTime = this.gunType.fireCoolLength();
        if (this.cycling) {
            this.cycling = false;
            this.cycleTime = 0;
        }
        return true;
    }

    @Override
    public boolean canTrigger() {
        return this.hammerReady
                && !this.leverDown
                && (!this.cycling || this.cycleTime <= this.gunType.cycleCancelableLength())
                && (!this.reloading || this.reloadTime <= this.gunType.reloadCancelableLength())
                && this.fireCoolTime == 0
                && this.cycleCoolTime == 0
                && this.reloadCoolTime == 0;
    }

    // CyclingLeverインターフェース実装
    private void cycleTick(LeverActionPlaySoundContext playSoundContext, CycleTickContext context, float timeDelta) {
        this.cycleTime = Math.max(0, this.cycleTime - timeDelta);
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
    public boolean cycle(LeverActionPlaySoundContext playSoundContext, AnimationContext animationContext) {
        if (!canCycle()) {
            return false;
        }
        if (this.chamber.isInCartridge()) {
            animationContext.setAnimation("cycle", this.leverDown ? this.gunType.leverDownLength() : 0);
        } else {
            animationContext.setAnimation("cycle_empty", this.leverDown ? this.gunType.leverDownLength() : 0);
        }
        playSoundContext.playSound(LeverActionPlaySoundContext.Sound.CYCLE);
        this.cycling = true;
        this.reloading = false;
        if (this.leverDown) {
            this.cycleTime = this.gunType.leverUpLength();
        } else {
            this.cycleTime = this.gunType.leverDownLength();
        }
        return true;
    }

    @Override
    public boolean canCycle() {
        return !this.cycling
                && this.cycleCoolTime == 0
                && this.reloadCoolTime == 0
                && this.fireCoolTime == 0
                && (!this.reloading || this.reloadTime <= this.gunType.reloadCancelableLength());
    }

    @Override
    public boolean shouldCycle() {
        return (this.chamber.canShoot() && (leverDown || !hammerReady))  // 撃てるが状態がおかしい時
                || (!this.chamber.canShoot() && this.magazine.hasBullet());  // 撃てないが弾はあるとき
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
    private void reloadTick(LeverActionPlaySoundContext playSoundContext, ReloadTickContext context, float timeDelta) {
        this.reloadTime = Math.max(0, this.reloadTime - timeDelta);
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
    public boolean reload(LeverActionPlaySoundContext playSoundContext, AnimationContext animationContext, ReloadStartContext context) {
        if (!canReload(context)) {
            return false;
        }
        int maxBullets = this.magazine.getMaxCapacity();
        int bullets = this.magazine.getBullets().size();
        if (bullets + this.gunType.reloadCount() >= maxBullets) {
            animationContext.setAnimation("reload_end", 0);
        } else {
            animationContext.setAnimation("reload", 0);
        }
        playSoundContext.playSound(LeverActionPlaySoundContext.Sound.RELOAD);
        this.reloading = true;
        this.cycling = false;
        this.reloadTime = this.gunType.reloadLength();
        return true;
    }

    @Override
    public boolean canReload(ReloadStartContext context) {
        return !this.reloading
                && this.cycleCoolTime == 0
                && this.reloadCoolTime == 0
                && this.fireCoolTime == 0
                // リロードはサイクルをキャンセルできない
                && !this.cycling
                && this.magazine.canAddBullet()
                && context.hasBullet(this.magazine.getMagazineType().allowBullet());
    }

    @Override
    public boolean shouldReload() {
        return this.magazine.canAddBullet();
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
        nbt.putFloat("cycleCoolTime", this.cycleCoolTime);
        nbt.putFloat("reloadTime", this.reloadTime);
        var chamberNbt = new NbtCompound();
        this.chamber.write(chamberNbt);
        nbt.put("chamber", chamberNbt);
        var magazineNbt = new NbtCompound();
        this.magazine.write(magazineNbt);
        nbt.put("magazine", magazineNbt);
    }

    // getter / setter

    public LeverActionGunDataType getGunType() {
        return gunType;
    }

    public Chamber getChamber() {
        return chamber;
    }

    public MagazineComponent getMagazine() {
        return magazine;
    }

    public void setHammerReady(boolean hammerReady) {
        this.hammerReady = hammerReady;
    }

    public void setLeverDown(boolean leverDown) {
        this.leverDown = leverDown;
    }

    public boolean isCycling() {
        return cycling;
    }

    public void setCycling(boolean cycling) {
        this.cycling = cycling;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public float getCycleTime() {
        return cycleTime;
    }

    public void setCycleTime(float cycleTime) {
        this.cycleTime = cycleTime;
    }

    public float getReloadTime() {
        return reloadTime;
    }

    public void setReloadTime(float reloadTime) {
        this.reloadTime = reloadTime;
    }

    public float getFireCoolTime() {
        return fireCoolTime;
    }

    public void setFireCoolTime(float fireCoolTime) {
        this.fireCoolTime = fireCoolTime;
    }

    public float getCycleCoolTime() {
        return cycleCoolTime;
    }

    public void setCycleCoolTime(float cycleCoolTime) {
        this.cycleCoolTime = cycleCoolTime;
    }

    public float getReloadCoolTime() {
        return reloadCoolTime;
    }

    public void setReloadCoolTime(float reloadCoolTime) {
        this.reloadCoolTime = reloadCoolTime;
    }
}

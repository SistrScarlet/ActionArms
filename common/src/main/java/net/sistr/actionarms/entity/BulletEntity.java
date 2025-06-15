package net.sistr.actionarms.entity;

import net.minecraft.entity.*;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sistr.actionarms.hud.BulletHitHudState;
import net.sistr.actionarms.item.component.BulletComponent;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;
import net.sistr.actionarms.mixin.DamageSourcesAccessor;
import net.sistr.actionarms.network.HudStatePacket;
import net.sistr.actionarms.setup.Registration;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BulletEntity extends Entity implements Ownable {
    @Nullable
    private UUID ownerId;
    @Nullable
    private Entity owner;
    private BulletComponent bulletComponent = GunComponentTypes.MIDDLE_CALIBER.get();
    private int decay = 40;

    public BulletEntity(EntityType<? extends BulletEntity> type, World world) {
        super(type, world);
    }

    public static BulletEntity of(EntityType<? extends BulletEntity> type, World world,
                                  @Nullable Entity owner, BulletComponent bulletComponent,
                                  Vec3d fireFrom, Vec3d fireFor, float speed) {
        var bullet = new BulletEntity(type, world);
        bullet.setPosition(fireFrom.x, fireFrom.y, fireFrom.z);
        bullet.setVelocity(fireFor.normalize().multiply(speed));

        Vec3d velocity = fireFor.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
        float horizontalLength = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float pitch = (float) Math.toDegrees(Math.atan2(-velocity.y, horizontalLength));
        bullet.setYaw(yaw);
        bullet.setPitch(pitch);
        bullet.prevYaw = yaw;
        bullet.prevPitch = pitch;

        if (owner != null) {
            bullet.owner = owner;
            bullet.ownerId = owner.getUuid();
        }
        bullet.bulletComponent = bulletComponent;
        return bullet;
    }

    @Override
    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.13f;
    }

    public static Vec3d getVec(float yaw, float pitch) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float g = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float h = -MathHelper.cos(-pitch * 0.017453292F);
        float i = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(g * h, i, f * h);
    }

    @Override
    protected void initDataTracker() {

    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("ownerId")) {
            this.ownerId = nbt.getUuid("ownerId");
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerId != null) {
            nbt.putUuid("ownerId", this.ownerId);
        }
    }

    @Override
    public void tick() {
        if (this.age > this.decay) {
            this.discard();
            return;
        }
        super.tick();
        // 現在の速度で移動した時に、物体(モブかブロック)とぶつかるかをチェック
        var start = this.getPos();
        var end = start.add(this.getVelocity());
        var hitResult = raycast(start, end);

        // ぶつかったらhit処理
        if (hitResult.getType() != HitResult.Type.MISS) {
            hit(hitResult);
        }

        var currentPos = this.getPos();
        var nextPos = currentPos.add(this.getVelocity());

        showTrailParticle(start, end);

        // 移動処理
        this.setPosition(nextPos);

        // 次tickのために速度を更新
        updateVelocity();
    }

    private HitResult raycast(Vec3d start, Vec3d end) {
        // 最初にブロックとの当たり判定をチェックする
        var bResult = this.getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (bResult.getType() != HitResult.Type.MISS) {
            start = bResult.getPos();
        }
        var eResult = ProjectileUtil.getEntityCollision(this.getWorld(), this, start, end,
                this.getBoundingBox().stretch(this.getVelocity()).expand(1.0),
                this::canHit,
                0.0f);
        return eResult != null ? eResult : bResult;
    }

    private boolean canHit(Entity entity) {
        var owner = this.getOwner();
        return entity != owner
                && (owner == null || !owner.isConnectedThroughVehicle(entity)
                && owner.getRootVehicle() != entity)
                && entity.isAttackable()
                && entity.canBeHitByProjectile();
    }

    private void hit(HitResult result) {
        boolean hit = false;
        if (result.getType() == HitResult.Type.ENTITY) {
            hit = entityHit((EntityHitResult) result);
        } else if (result.getType() == HitResult.Type.BLOCK) {
            hit = blockHit((BlockHitResult) result);
        }
        if (hit) {
            // velocityをヒット位置未満に更新
            var hitPos = result.getPos();
            var currentPos = this.getPos();
            var velocity = this.getVelocity();
            double distanceToHit = hitPos.distanceTo(currentPos);
            double totalDistance = velocity.length();
            if (distanceToHit < totalDistance) {
                this.setVelocity(velocity.normalize().multiply(distanceToHit));
            }

            // 消去
            this.discard();
        }
    }

    private boolean entityHit(EntityHitResult result) {
        var hitTarget = result.getEntity();
        var data = this.bulletComponent.getBulletDataType();
        float damage = isHeadshot(result) ? data.headshotDamage() : data.damage();

        if (hitTarget.damage(createDamageSource(), damage)) {
            var owner = getOwner();
            if (owner instanceof ServerPlayerEntity player) {
                boolean kill = !hitTarget.isAlive();
                HudStatePacket.sendS2C(player, "bullet_hit", BulletHitHudState.of(kill).write());
            }

            return true;
        }

        return false;
    }

    // todo:各モブでのヘッドショット判定の確認
    private boolean isHeadshot(EntityHitResult result) {
        var hitTarget = result.getEntity();
        var hitTargetBox = hitTarget.getBoundingBox();
        double targetWidth = hitTargetBox.maxX - hitTargetBox.minX;
        double targetHeight = hitTargetBox.maxY - hitTargetBox.minY;
        // 身長1以下は全身ヘッドショット判定
        if (targetHeight <= 1) {
            return true;
        }
        if (hitTarget instanceof EnderDragonPart part) {
            return part.name.equals("head");
        }
        // 横幅と高さが一定以下なら、頭部判定を作成し、それを通過していればヘッドショット判定とする
        if (targetWidth <= 2 && targetHeight <= 4) {
            double weakBoxSize = targetWidth;
            if (weakBoxSize > 1) {
                weakBoxSize = 1 + (weakBoxSize - 1) * 0.5;
            }
            weakBoxSize *= 0.8;
            Vec3d eyePos = hitTarget.getEyePos();
            var weakBox = Box.of(eyePos, weakBoxSize, weakBoxSize, weakBoxSize);
            var start = this.getPos();
            var end = start.add(this.getVelocity().multiply(2));
            return weakBox.raycast(start, end).isPresent();
        }

        // 大型の敵ならヘッドショット判定無しとする (仮)
        return false;
    }

    private DamageSource createDamageSource() {
        var damageSources = this.getDamageSources();
        var registry = ((DamageSourcesAccessor)damageSources).getRegistry();
        return new DamageSource(registry.entryOf(Registration.BULLET_DAMAGE_TYPE),
                this, this.getOwner());
    }

    private boolean blockHit(BlockHitResult result) {
        return true;
    }

    private void showTrailParticle(Vec3d start, Vec3d end) {
        float particleDistance = 1;
        var direction = end.subtract(start).normalize();
        double totalDistance = start.distanceTo(end);

        for (double distance = 0; distance < totalDistance; distance += particleDistance) {
            var particlePos = start.add(direction.multiply(distance));
            var velocity = this.getVelocity().multiply(0.5);
            this.getWorld().addParticle(ParticleTypes.CRIT, particlePos.getX(), particlePos.getY(), particlePos.getZ(),
                    velocity.getX(), velocity.getY(), velocity.getZ());
        }
    }

    private void updateVelocity() {
        Vec3d velocity = this.getVelocity();
        velocity = velocity.multiply(0.99f);
        velocity = velocity.add(0, -0.01f, 0);
        this.setVelocity(velocity);
    }


    @Override
    public @Nullable Entity getOwner() {
        if (this.owner != null && !this.owner.isRemoved()) {
            return this.owner;
        }
        if (this.ownerId != null && this.getWorld() instanceof ServerWorld serverWorld) {
            this.owner = serverWorld.getEntity(this.ownerId);
            return this.owner;
        }
        return null;
    }
}

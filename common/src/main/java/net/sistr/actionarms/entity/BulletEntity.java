package net.sistr.actionarms.entity;

import net.minecraft.entity.*;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
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
import net.sistr.actionarms.entity.util.EntityRecordManager;
import net.sistr.actionarms.entity.util.HasEntityRecordManager;
import net.sistr.actionarms.hud.BulletHitHudState;
import net.sistr.actionarms.item.component.BulletComponent;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;
import net.sistr.actionarms.mixin.DamageSourcesAccessor;
import net.sistr.actionarms.network.HudStatePacket;
import net.sistr.actionarms.setup.Registration;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

public class BulletEntity extends Entity implements Ownable {
    @Nullable
    private UUID ownerId;
    @Nullable
    private Entity owner;
    private BulletComponent bulletComponent = GunComponentTypes.MEDIUM_CALIBER_BULLET.get();
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
            end = bResult.getPos();
        }
        var eResult = getEntityCollision(this.getWorld(), this, start, end,
                this.getBoundingBox().stretch(this.getVelocity()).expand(4.0),
                this::canHit,
                0.01f);
        return eResult != null ? eResult : bResult;
    }

    // EntityRecordベースの連続衝突判定
    private static @Nullable ExtendEntityHitResult getEntityCollision(World world, Entity projectile, Vec3d start, Vec3d end,
                                                                      Box hittableBox, Predicate<Entity> predicate, float size) {
        if (!(world instanceof ServerWorld)) {
            return null;
        }
        double nearestTime = Double.MAX_VALUE;
        Entity nearestEntity = null;
        for (Entity entity : world.getOtherEntities(projectile, hittableBox, predicate)) {
            // ターゲットのボックスと速度を取得する
            // HasEntityRecordManagerはServerWorldだけが実装しているため注意
            var entityRecordManager = ((HasEntityRecordManager) world).actionArms$getEntityRecordManager();

            // 過去の状態を取得して、クライアント/サーバーラグを補償する
            EntityRecordManager.EntityRecord oldRecord;
            EntityRecordManager.EntityRecord newRecord;

            var olderTick = entityRecordManager.getRecord(entity.getUuid(), 5);
            var oldTick = entityRecordManager.getRecord(entity.getUuid(), 4);

            if (olderTick.isPresent() && oldTick.isPresent()) {
                // 理想的なケース：5tick前と4tick前のデータが両方存在
                oldRecord = olderTick.get();
                newRecord = oldTick.get();
            } else if (oldTick.isPresent()) {
                // 5tick前のデータがない場合：4tick前 → 現在の速度を計算
                oldRecord = oldTick.get();
                newRecord = new EntityRecordManager.EntityRecord(
                        entity.getUuid(),
                        entity.getPos(),
                        entity.getBoundingBox(),
                        entity.getEyePos()
                );
            } else {
                // どちらもない場合：速度は0として扱う
                oldRecord = new EntityRecordManager.EntityRecord(
                        entity.getUuid(),
                        entity.getPos(),
                        entity.getBoundingBox(),
                        entity.getEyePos()
                );
                newRecord = oldRecord;
            }

            var entityVelocity = newRecord.pos().subtract(oldRecord.pos());
            var entityBox = oldRecord.boundingBox();
            var result = BulletEntity.CollisionDetector.detectCollision(start, size,
                    end.subtract(start),
                    entityBox, entityVelocity);
            // ヒットして、なおかつ最も近いなら更新
            if (result.isCollisionCurrentTick()
                    && result.collisionTime() < nearestTime) {
                nearestEntity = entity;
                nearestTime = result.collisionTime();
            }
        }
        if (nearestEntity == null) {
            return null;
        }
        return new ExtendEntityHitResult(nearestEntity, nearestTime);
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
            hit = entityHit((ExtendEntityHitResult) result);
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

    private boolean entityHit(ExtendEntityHitResult result) {
        var hitTarget = result.getEntity();
        var data = this.bulletComponent.getBulletDataType();
        boolean isHeadshot = isHeadshot(result);
        float damage = isHeadshot ? data.headshotDamage() : data.damage();

        if (hitTarget.damage(createDamageSource(), damage)) {
            var owner = getOwner();
            if (owner instanceof ServerPlayerEntity player) {
                boolean kill = !hitTarget.isAlive();
                var state = BulletHitHudState.State.of(kill, isHeadshot);
                HudStatePacket.sendS2C(player,
                        "bullet_hit",
                        BulletHitHudState.of(state).write()
                );
            }

            return true;
        }

        return false;
    }

    private boolean isHeadshot(ExtendEntityHitResult result) {
        var hitTarget = result.getEntity();
        if (!(this.getWorld() instanceof ServerWorld)) {
            return false;
        }

        var entityRecordManager = ((HasEntityRecordManager) this.getWorld()).actionArms$getEntityRecordManager();
        var olderTick = entityRecordManager.getRecord(hitTarget.getUuid(), 5);
        var oldTick = entityRecordManager.getRecord(hitTarget.getUuid(), 4);

        EntityRecordManager.EntityRecord record;
        if (olderTick.isPresent() && oldTick.isPresent()) {
            // Linear interpolation between older and old records using hitTime
            double t = result.getHitTime();
            var older = olderTick.get();
            var old = oldTick.get();

            var pos = older.pos().lerp(old.pos(), t);
            var box = new Box(
                    lerp(older.boundingBox().minX, old.boundingBox().minX, t),
                    lerp(older.boundingBox().minY, old.boundingBox().minY, t),
                    lerp(older.boundingBox().minZ, old.boundingBox().minZ, t),
                    lerp(older.boundingBox().maxX, old.boundingBox().maxX, t),
                    lerp(older.boundingBox().maxY, old.boundingBox().maxY, t),
                    lerp(older.boundingBox().maxZ, old.boundingBox().maxZ, t)
            );
            var eyePos = older.eyePos().lerp(old.eyePos(), t);

            record = new EntityRecordManager.EntityRecord(hitTarget.getUuid(), pos, box, eyePos);
        } else if (oldTick.isPresent()) {
            record = oldTick.get();
        } else {
            record = new EntityRecordManager.EntityRecord(
                    hitTarget.getUuid(),
                    hitTarget.getPos(),
                    hitTarget.getBoundingBox(),
                    hitTarget.getEyePos()
            );
        }

        var hitTargetBox = record.boundingBox();
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
            Vec3d eyePos = record.eyePos();
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
        var registry = ((DamageSourcesAccessor) damageSources).getRegistry();
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
    public boolean isAttackable() {
        return false;
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

    public record CollisionResult(boolean hasCollision, double collisionTime) {
        public static final CollisionResult NONE = new CollisionResult(false, -1);

        public boolean isCollisionCurrentTick() {
            return hasCollision && collisionTime() <= 1;
        }
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    public static class CollisionDetector {
        private static final double EPSILON = 1.0e-9;

        /**
         * 球体とAABBの連続衝突検出
         *
         * @param sp          球体座標
         * @param size        球体サイズ
         * @param spVelocity  球体の速度
         * @param box         AABB
         * @param boxVelocity AABBの速度
         * @return 衝突結果（衝突の有無と衝突時間）
         */
        public static CollisionResult detectCollision(Vec3d sp, double size, Vec3d spVelocity,
                                                      Box box, Vec3d boxVelocity) {
            // 相対速度を計算（球がAABBに対してどう動くかを計算）
            Vec3d relVel = spVelocity.subtract(boxVelocity);


            // 球の半径分だけAABBを拡張
            box = box.expand(size);

            // 各軸での衝突時間区間を計算
            double[] xInterval = calculateAxisInterval(sp.x, relVel.getX(), box.minX, box.maxX);
            double[] yInterval = calculateAxisInterval(sp.y, relVel.getY(), box.minY, box.maxY);
            double[] zInterval = calculateAxisInterval(sp.z, relVel.getZ(), box.minZ, box.maxZ);

            if (xInterval == null || yInterval == null || zInterval == null) {
                return CollisionResult.NONE;
            }

            // 3軸の時間区間の交集合を計算
            double tEnter = Math.max(Math.max(xInterval[0], yInterval[0]), zInterval[0]);
            double tExit = Math.min(Math.min(xInterval[1], yInterval[1]), zInterval[1]);

            // 交集合が存在し、かつ未来の時間であれば衝突
            boolean hasCollision = tEnter <= tExit && tExit >= 0;
            if (!hasCollision) return CollisionResult.NONE;

            double collisionTime = Math.max(0, tEnter);

            return new CollisionResult(true, collisionTime);
        }

        /**
         * 1軸での衝突時間区間を計算
         *
         * @param pos 球の中心位置（該当軸）
         * @param vel 相対速度（該当軸）
         * @param min 拡張されたAABBの最小値
         * @param max 拡張されたAABBの最大値
         * @return 時間区間 [tEnter, tExit]、衝突しない場合はnull
         */
        private static double @Nullable [] calculateAxisInterval(double pos, double vel, double min, double max) {
            // 既に範囲内にいる場合は常に衝突
            if (min <= pos && pos <= max) {
                return new double[]{0, Double.POSITIVE_INFINITY};
            }
            // 範囲外かつ速度0なら永続的に衝突しない
            if (Math.abs(vel) < EPSILON) {
                return null;
            }

            // 速度がある場合の時間計算
            double t1 = (min - pos) / vel;
            double t2 = (max - pos) / vel;

            // 時間の順序を正しくする
            double tEnter = Math.min(t1, t2);
            double tExit = Math.max(t1, t2);

            return new double[]{tEnter, tExit};
        }
    }

    public static class ExtendEntityHitResult extends EntityHitResult {
        private final double hitTime;

        public ExtendEntityHitResult(Entity entity, double hitTime) {
            super(entity);
            this.hitTime = hitTime;
        }

        public double getHitTime() {
            return hitTime;
        }
    }
}
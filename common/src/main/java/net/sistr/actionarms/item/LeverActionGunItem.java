package net.sistr.actionarms.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterials;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.sistr.actionarms.entity.BulletEntity;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.entity.util.IAimManager;
import net.sistr.actionarms.entity.util.InventoryAmmoUtil;
import net.sistr.actionarms.item.component.*;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;
import net.sistr.actionarms.network.RecoilPacket;
import net.sistr.actionarms.setup.Registration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LeverActionGunItem extends GunItem {
    private final Supplier<LeverActionGunComponent> gunComponentSupplier;

    public LeverActionGunItem(Settings settings, Supplier<LeverActionGunComponent> gunComponentSupplier) {
        super(settings);
        this.gunComponentSupplier = gunComponentSupplier;
    }

    // Item継承メソッド

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ToolMaterials.IRON.getRepairIngredient().test(ingredient) || super.canRepair(stack, ingredient);
    }

    @Override
    public int getEnchantability() {
        return ToolMaterials.IRON.getEnchantability();
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        var gunComponent = IItemComponent.query(getGunComponent(), stack, c -> c);
        // 適合弾薬
        tooltip.add(Text.translatable("item.actionarms.gun.loadable_ammo"));
        tooltip.add(Text.translatable("item.actionarms.gun.loadable_ammo.list",
                Text.translatable("item.actionarms.medium_caliber_bullet")).formatted(Formatting.GRAY));

        // チャンバー状態
        if (gunComponent.getChamber().canShoot()) {
            tooltip.add(Text.translatable("item.actionarms.gun.chamber.loaded").formatted(Formatting.YELLOW));
        } else if (!gunComponent.getChamber().isEmpty()) {
            tooltip.add(Text.translatable("item.actionarms.gun.chamber.empty_caliber").formatted(Formatting.YELLOW));
        } else {
            tooltip.add(Text.translatable("item.actionarms.gun.chamber.empty").formatted(Formatting.GRAY));
        }

        // マガジン情報
        var magazine = gunComponent.getMagazine();
        int currentAmmo = magazine.getBullets().size();
        int maxAmmo = magazine.getMaxCapacity();
        tooltip.add(Text.translatable("item.actionarms.gun.magazine.ammo", currentAmmo, maxAmmo)
                .formatted(currentAmmo > 0 ? Formatting.WHITE : Formatting.GRAY));


        // デバッグ表示時
        if (context.isAdvanced()) {
            // デバッグ情報をツールチップに追加する
        }
    }

    // 処理

    public void fireBullet(ServerWorld world, LivingEntity user,
                           LeverActionGunComponent gunComponent, BulletComponent bullet) {
        // 射撃処理
        var fireDirection = user.getRotationVector();
        float fireSpread = fireSpread(user, gunComponent);
        fireDirection = calculateSpreadDirection(fireDirection, fireSpread, user.getRandom());

        var bulletEntity = BulletEntity.of(Registration.BULLET_ENTITY.get(), world, user,
                bullet, user.getEyePos().add(0, -0.1, 0), fireDirection, 3);
        world.spawnEntity(bulletEntity);

        // 発砲煙パーティクル
        // プレイヤーの少し前方にパーティクルを生成する
        var eyePos = user.getEyePos();
        var lookVec = user.getRotationVector();

        double particleDistance = 1;
        double particleRange = 0.25;

        var particlePos = eyePos.add(lookVec.multiply(particleDistance));

        world.spawnParticles(ParticleTypes.SMOKE, particlePos.getX(), particlePos.getY(), particlePos.getZ(),
                10, particleRange, particleRange, particleRange, 0.05);

        // リコイル
        if (user instanceof ServerPlayerEntity player) {
            RecoilPacket.sendS2C(player);
        }
    }

    public float fireSpread(LivingEntity user, LeverActionGunComponent gunComponent) {
        var gunData = gunComponent.getGunType();
        float baseSpread = gunData.baseSpreadAngle();

        // エイム状態チェック（既存システム活用）
        boolean isAiming = HasAimManager.get(user)
                .map(IAimManager::isAiming)
                .orElse(false);

        // 現在の拡散角計算
        float currentSpread = baseSpread;
        if (isAiming) {
            currentSpread = gunData.aimSpreadAngle();
        }

        boolean isUserFly = user instanceof PlayerEntity player && player.getAbilities().flying;

        // 移動状態による拡散増加
        boolean isMoving = user.getVelocity().horizontalLengthSquared() > 0.01
                || (!user.isOnGround() && isUserFly);
        if (isMoving) {
            currentSpread += gunData.movementSpreadIncrease();
        }
        return currentSpread;
    }

    /**
     * 拡散角を考慮した射撃方向ベクトルを計算する
     * 真上・真下向きでも正しく拡散するように3D直交座標系を構築
     *
     * @param baseDirection      基本射撃方向（正規化済み）
     * @param spreadAngleDegrees 拡散角（度）
     * @param random             ランダム生成器
     * @return 拡散を適用した射撃方向ベクトル（正規化済み）
     */
    public static Vec3d calculateSpreadDirection(Vec3d baseDirection, float spreadAngleDegrees, Random random) {
        // 拡散角が0なら元の方向をそのまま返す
        if (spreadAngleDegrees <= 0) {
            return baseDirection;
        }

        // 拡散角をラジアンに変換
        double spreadAngleRad = Math.toRadians(spreadAngleDegrees);

        // 基本方向ベクトルを正規化（念のため）
        Vec3d forward = baseDirection.normalize();

        // 前方ベクトルに垂直な2つのベクトル（右、上）を計算
        Vec3d right = calculateRightVector(forward);
        Vec3d up = forward.crossProduct(right).normalize();

        // 拡散円内でランダムな点を生成（極座標）
        double spreadRadius = random.nextDouble() * Math.tan(spreadAngleRad);
        double spreadAngle = random.nextDouble() * 2.0 * Math.PI;

        // 極座標を直交座標に変換
        double spreadX = spreadRadius * Math.cos(spreadAngle);
        double spreadY = spreadRadius * Math.sin(spreadAngle);

        // ローカル座標系（前方1、右spreadX、上spreadY）を世界座標系に変換
        Vec3d spreadDirection = forward
                .add(right.multiply(spreadX))
                .add(up.multiply(spreadY));

        return spreadDirection.normalize();
    }

    /**
     * 前方ベクトルに垂直な右ベクトルを計算
     * 真上・真下問題を解決するための特別処理
     */
    private static Vec3d calculateRightVector(Vec3d forward) {
        // 前方ベクトルが真上または真下に近い場合の特別処理
        double threshold = 0.99; // cos(約8度)

        if (Math.abs(forward.y) > threshold) {
            // 真上・真下の場合：Z軸を基準にした右ベクトルを使用
            return new Vec3d(1.0, 0.0, 0.0);
        } else {
            // 一般的な場合：世界座標のY軸（上）とのクロス積で右ベクトルを計算
            Vec3d worldUp = new Vec3d(0.0, 1.0, 0.0);
            return forward.crossProduct(worldUp).normalize();
        }
    }

    // getter

    public Supplier<LeverActionGunComponent> getGunComponent() {
        return this.gunComponentSupplier;
    }

    // コンテキスト生成

    public LeverActionPlaySoundContext createPlaySoundContext(World world, Entity entity) {
        return sound -> sound.playSound(world, entity, SoundCategory.PLAYERS);
    }

    public AnimationContext createAnimationContext(World world, UUID uuid) {
        return AnimationContext.of(world, uuid);
    }

    public FireTrigger.FireStartContext createFireStartContext(World world, LivingEntity user) {
        return (gun, bullet) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }
            fireBullet(serverWorld, user, gun, bullet);
        };
    }

    public CyclingLever.CycleTickContext createCycleTickContext() {
        return cartridge -> {
            // todo 排莢処理
        };
    }

    public Reloadable.ReloadStartContext createReloadStartContext(LivingEntity user) {
        if (!(user instanceof PlayerEntity)) {
            return (Predicate<BulletComponent> predicate) -> true;
        }
        return (Predicate<BulletComponent> predicate) ->
                ((PlayerEntity) user).isCreative()
                        || InventoryAmmoUtil.hasBullet(((PlayerEntity) user).getInventory(), predicate);

    }

    public Reloadable.ReloadTickContext createReloadTickContext(Entity user) {
        return new LeverActionReloadTickContext(user);
    }


    private record LeverActionReloadTickContext(Entity user) implements Reloadable.ReloadTickContext {

        @Override
        public List<BulletComponent> popBullets(Predicate<BulletComponent> predicate, int count) {
            // プレイヤーかつクリエイティブでないならインベントリから弾を取り出す。
            if (user instanceof PlayerEntity player && !player.isCreative()) {
                return InventoryAmmoUtil.popBullets(player.getInventory(), predicate, count);
            }
            // そうでないなら無限
            List<BulletComponent> bullets = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                bullets.add(GunComponentTypes.MEDIUM_CALIBER_BULLET.get());
            }
            return bullets;
        }

        @Override
        public void returnBullets(List<BulletComponent> bullets) {
            // todo 弾薬返還処理
        }
    }
}

package net.sistr.actionarms.item;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterials;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.sistr.actionarms.entity.BulletEntity;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.entity.util.IAimManager;
import net.sistr.actionarms.item.component.IComponent;
import net.sistr.actionarms.item.component.SAAGunComponent;
import net.sistr.actionarms.item.data.BulletData;
import net.sistr.actionarms.item.data.SAAGunData;
import net.sistr.actionarms.item.util.AnimationContext;
import net.sistr.actionarms.item.util.SAAPlaySoundContext;
import net.sistr.actionarms.network.RecoilPacket;
import net.sistr.actionarms.setup.Registration;
import org.jetbrains.annotations.Nullable;

public class SAAGunItem extends GunItem {
    private final SAAGunData gunData;

    public SAAGunItem(Settings settings, SAAGunData gunData) {
        super(settings);
        this.gunData = gunData;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ToolMaterials.IRON.getRepairIngredient().test(ingredient)
                || super.canRepair(stack, ingredient);
    }

    @Override
    public int getEnchantability() {
        return ToolMaterials.IRON.getEnchantability();
    }

    @Override
    public void appendTooltip(
            ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // 適合弾薬
        tooltip.add(Text.translatable("item.actionarms.gun.loadable_ammo"));
        tooltip.add(
                Text.translatable(
                                "item.actionarms.gun.loadable_ammo.list",
                                Text.translatable("item.actionarms.medium_caliber_bullet"))
                        .formatted(Formatting.GRAY));

        // シリンダー情報
        var gunComponent = IComponent.query(getGunComponent(), stack, c -> c);
        var cylinder = gunComponent.getCylinder();
        int loaded = cylinder.countLoaded();
        int capacity = cylinder.getCapacity();
        tooltip.add(
                Text.translatable("item.actionarms.gun.cylinder.ammo", loaded, capacity)
                        .formatted(loaded > 0 ? Formatting.WHITE : Formatting.GRAY));
    }

    public void fireBullet(
            ServerWorld world, LivingEntity user, SAAGunComponent gunComponent, BulletData bullet) {
        var fireDirection = user.getRotationVector();
        float fireSpread = fireSpread(user, gunComponent);
        fireDirection =
                LeverActionGunItem.calculateSpreadDirection(
                        fireDirection, fireSpread, user.getRandom());

        var bulletEntity =
                BulletEntity.of(
                        Registration.BULLET_ENTITY.get(),
                        world,
                        user,
                        bullet,
                        user.getEyePos().add(0, -0.1, 0),
                        fireDirection,
                        3);
        world.spawnEntity(bulletEntity);

        // 発砲煙パーティクル
        var eyePos = user.getEyePos();
        var lookVec = user.getRotationVector();
        double particleDistance = 0.8;
        double particleRange = 0.2;
        var particlePos = eyePos.add(lookVec.multiply(particleDistance));
        world.spawnParticles(
                ParticleTypes.SMOKE,
                particlePos.getX(),
                particlePos.getY(),
                particlePos.getZ(),
                8,
                particleRange,
                particleRange,
                particleRange,
                0.05);

        if (user instanceof ServerPlayerEntity player) {
            RecoilPacket.sendS2C(player);
        }
    }

    public float fireSpread(LivingEntity user, SAAGunComponent gunComponent) {
        float baseSpread = gunData.baseSpreadAngle();
        boolean isAiming = HasAimManager.get(user).map(IAimManager::isAiming).orElse(false);
        float currentSpread = isAiming ? gunData.aimSpreadAngle() : baseSpread;

        boolean isUserFly = user instanceof PlayerEntity player && player.getAbilities().flying;
        boolean isMoving =
                user.getVelocity().horizontalLengthSquared() > 0.01
                        || (!user.isOnGround() && !isUserFly);
        if (isMoving) {
            currentSpread += gunData.movementSpreadIncrease();
        }
        return currentSpread;
    }

    public Supplier<SAAGunComponent> getGunComponent() {
        return () -> new SAAGunComponent(this.gunData);
    }

    public SAAGunComponent.SoundContext createSoundContext(World world, LivingEntity user) {
        return (SAAPlaySoundContext)
                sound -> {
                    var mapped = SAAPlaySoundContext.SOUND_MAP.get(sound);
                    if (mapped != null) {
                        mapped.playSound(world, user, net.minecraft.sound.SoundCategory.PLAYERS);
                    }
                };
    }

    public AnimationContext createAnimationContext(World world, UUID uuid) {
        return AnimationContext.of(world, uuid);
    }

    public SAAGunComponent.FireContext createFireContext(World world, LivingEntity user) {
        return (gun, bullet) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }
            fireBullet(serverWorld, user, gun, bullet);
        };
    }

    public SAAGunData getGunData() {
        return gunData;
    }
}

package net.sistr.actionarms.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import net.sistr.actionarms.entity.BulletEntity;
import net.sistr.actionarms.entity.util.InventoryAmmoUtil;
import net.sistr.actionarms.item.component.*;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;
import net.sistr.actionarms.network.ItemAnimationEventPacket;
import net.sistr.actionarms.setup.Registration;

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

    public LeverActionPlaySoundContext createPlaySoundContext(World world, Entity entity) {
        return sound -> sound.playSound(world, entity, SoundCategory.PLAYERS);
    }

    public AnimationContext createAnimationContext(World world, UUID uuid) {
        return AnimationContext.of(world, uuid);
    }

    public FireTrigger.FireStartContext createFireStartContext(World world, LivingEntity user, UUID uuid) {
        return bullet -> {
            // 射撃処理
            var bulletEntity = BulletEntity.of(Registration.BULLET_ENTITY.get(), world, user,
                    bullet, user.getEyePos(), BulletEntity.getVec(user.getYaw(), user.getPitch()), 3);
            world.spawnEntity(bulletEntity);

            ItemAnimationEventPacket.sendS2C(world, uuid, "firing", 0);
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
        return new Reloadable.ReloadTickContext() {
            @Override
            public List<BulletComponent> popBullets(Predicate<BulletComponent> predicate, int count) {
                // プレイヤーかつクリエイティブでないならインベントリから弾を取り出す。
                if (user instanceof PlayerEntity player && !player.isCreative()) {
                    return InventoryAmmoUtil.popBullets(player.getInventory(), predicate, count);
                }
                // そうでないなら無限
                List<BulletComponent> bullets = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    bullets.add(GunComponentTypes.MIDDLE_CALIBER.get());
                }
                return bullets;
            }

            @Override
            public void returnBullets(List<BulletComponent> bullets) {
                // todo 弾薬返還処理
            }
        };
    }

    public Supplier<LeverActionGunComponent> getGunComponent() {
        return this.gunComponentSupplier;
    }
}

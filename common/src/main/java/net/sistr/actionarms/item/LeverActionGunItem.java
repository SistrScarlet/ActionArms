package net.sistr.actionarms.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
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

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient) {
            return;
        }
        boolean isSelected = entity instanceof LivingEntity
                && ((LivingEntity) entity).getStackInHand(Hand.MAIN_HAND) == stack;
        IItemComponent.execute(getGunComponent(), stack, component -> {
            LeverActionPlaySoundContext playSoundContext = createPlaySoundContext(world, entity);
            component.tick(playSoundContext,
                    createCycleTickContext(),
                    createReloadTickContext(entity),
                    1f / 20f,
                    isSelected
            );
            return IItemComponent.ComponentResult.MODIFIED;
        });
        ItemUniqueManager.INSTANCE.uniqueCheck(world, stack);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return super.use(world, user, hand);
        }
        var stack = user.getStackInHand(hand);
        var uuid = UniqueComponent.get(stack);
        IItemComponent.execute(getGunComponent(), stack, gun -> {
            var animationContext = createAnimationContext(world, uuid);
            LeverActionPlaySoundContext playSoundContext = createPlaySoundContext(world, user);
            if (gun.shouldCycle()) {
                if (gun.canCycle()) {
                    if (gun.cycle(playSoundContext, animationContext)) {
                        return IItemComponent.ComponentResult.MODIFIED;
                    }
                }
            }
            Reloadable.ReloadStartContext reloadContext = createReloadStartContext(user);
            if (gun.shouldReload(reloadContext)) {
                if (gun.canReload(reloadContext)) {
                    if (gun.reload(playSoundContext, animationContext, reloadContext)) {
                        return IItemComponent.ComponentResult.MODIFIED;
                    }
                }
            }
            if (gun.canTrigger()) {
                boolean result = gun.trigger(playSoundContext, animationContext, createFireStartContext(world, user, uuid));
                if (result) {
                    return IItemComponent.ComponentResult.MODIFIED;
                } else {
                    return IItemComponent.ComponentResult.NO_CHANGE;
                }
            }
            return IItemComponent.ComponentResult.NO_CHANGE;
        });
        return super.use(world, user, hand);
    }

    private LeverActionPlaySoundContext createPlaySoundContext(World world, Entity entity) {
        return sound -> sound.playSound(world, entity, SoundCategory.PLAYERS);
    }

    private AnimationContext createAnimationContext(World world, UUID uuid) {
        return AnimationContext.of(world, uuid);
    }

    private FireTrigger.FireStartContext createFireStartContext(World world, PlayerEntity user, UUID uuid) {
        return bullet -> {
            // 射撃処理
            var bulletEntity = BulletEntity.of(Registration.BULLET_ENTITY.get(), world, user,
                    bullet, user.getEyePos(), BulletEntity.getVec(user.getYaw(), user.getPitch()), 3);
            world.spawnEntity(bulletEntity);

            ItemAnimationEventPacket.sendS2C(world, uuid, "firing", 0);
        };
    }

    private CyclingLever.CycleTickContext createCycleTickContext() {
        return cartridge -> {
            // todo 排莢処理
        };
    }

    private Reloadable.ReloadStartContext createReloadStartContext(PlayerEntity user) {
        return (Predicate<BulletComponent> predicate) ->
                user.isCreative() ||
                        InventoryAmmoUtil.hasBullet(user.getInventory(), predicate);
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

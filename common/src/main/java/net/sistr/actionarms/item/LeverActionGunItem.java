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
import net.sistr.actionarms.item.component.*;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;
import net.sistr.actionarms.network.ItemAnimationEventPacket;

import java.util.ArrayList;
import java.util.List;
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
            component.tick(
                    cartridge -> {
                        // todo 排莢処理
                    },
                    new Reloadable.ReloadTickContext() {
                        @Override
                        public List<BulletComponent> popBullets(Predicate<BulletComponent> predicate, int count) {
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
                    },
                    1f / 20f,
                    isSelected
            );
            return IItemComponent.ComponentResult.MODIFIED;
        });
        ItemUniqueManager.INSTANCE.uniqueCheck(world, stack);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            var stack = user.getStackInHand(hand);
            var uuid = UniqueComponent.get(stack);
            var animationContext = AnimationContext.of(world, uuid);
            IItemComponent.execute(getGunComponent(), stack, gun -> {
                if (gun.shouldCycle()) {
                    if (gun.canCycle()) {
                        if (gun.cycle(animationContext)) {
                            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                                    SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                        }
                        return IItemComponent.ComponentResult.MODIFIED;
                    }
                }
                if (gun.shouldReload()) {
                    if (gun.canReload()) {
                        if (gun.reload(animationContext)) {
                            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                                    SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                        }

                        return IItemComponent.ComponentResult.MODIFIED;
                    }
                }
                if (gun.canTrigger()) {
                    boolean result = gun.trigger(animationContext, bullet -> {
                        ItemAnimationEventPacket.sendS2C(world, uuid, "firing", 0);
                        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    });
                    if (result) {
                        return IItemComponent.ComponentResult.MODIFIED;
                    } else {
                        return IItemComponent.ComponentResult.NO_CHANGE;
                    }
                }
                return IItemComponent.ComponentResult.NO_CHANGE;
            });
        }
        return super.use(world, user, hand);
    }

    public Supplier<LeverActionGunComponent> getGunComponent() {
        return this.gunComponentSupplier;
    }

}

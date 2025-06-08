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
import net.sistr.actionarms.component.*;
import net.sistr.actionarms.item.component.IItemComponent;
import net.sistr.actionarms.item.component.LeverActionGunComponent;
import net.sistr.actionarms.item.component.UniqueComponent;
import net.sistr.actionarms.network.ItemAnimationEventPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class LeverActionGunItem extends GunItem {
    public LeverActionGunItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient) {
            return;
        }
        boolean isSelected = entity instanceof LivingEntity
                && ((LivingEntity) entity).getStackInHand(Hand.MAIN_HAND) == stack;
        IItemComponent.execute(LeverActionGunComponent::new, stack, component -> {
            component.leverActionGunItem.tick(
                    isSelected,
                    new CyclingLever.CycleTickContext() {
                        @Override
                        public void ejectCartridge(Cartridge cartridge) {
                            // todo 排莢処理
                        }
                    },
                    new Reloadable.ReloadTickContext() {
                        @Override
                        public List<Bullet> popBullets(Predicate<Bullet> predicate, int count) {
                            List<Bullet> bullets = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                bullets.add(new Bullet(BulletType.DEFAULT_TYPE));
                            }
                            return bullets;
                        }

                        @Override
                        public void returnBullets(List<Bullet> bullets) {
                            // todo 弾薬返還処理
                        }
                    },
                    1f / 20f
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
            IItemComponent.execute(LeverActionGunComponent::new, stack, component -> {
                var gun = component.leverActionGunItem;
                if (gun.shouldCycle()) {
                    if (gun.canCycle()) {
                        if (gun.cycle(s -> ItemAnimationEventPacket.sendS2C(world, uuid, "cycle", s))) {
                            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                                    SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                        }
                        return IItemComponent.ComponentResult.MODIFIED;
                    }
                }
                if (gun.shouldReload()) {
                    if (gun.canReload()) {
                        if (gun.reload(s -> ItemAnimationEventPacket.sendS2C(world, uuid, "reload", s))) {
                            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                                    SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                        }

                        return IItemComponent.ComponentResult.MODIFIED;
                    }
                }
                if (gun.canTrigger()) {
                    boolean result = gun.trigger(bullet -> {
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


}

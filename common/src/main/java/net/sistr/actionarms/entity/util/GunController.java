package net.sistr.actionarms.entity.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.ItemUniqueManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.IItemComponent;
import net.sistr.actionarms.item.component.LeverActionPlaySoundContext;
import net.sistr.actionarms.item.component.Reloadable;
import net.sistr.actionarms.item.component.UniqueComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class GunController {
    private final LivingEntity user;
    private final IKeyInputManager keyInputManager;
    private final Supplier<List<ItemStack>> itemsSupplier;

    public GunController(LivingEntity user, IKeyInputManager keyInputManager, Supplier<List<ItemStack>> itemsSupplier) {
        this.user = user;
        this.keyInputManager = keyInputManager;
        this.itemsSupplier = itemsSupplier;
    }

    public void tick() {
        if (this.user.getWorld().isClient) {
            return;
        }
        var main = user.getMainHandStack();

        var stacks = itemsSupplier.get();
        for (ItemStack stack : stacks) {
            if (!(stack.getItem() instanceof LeverActionGunItem leverAction)) {
                continue;
            }
            ItemUniqueManager.INSTANCE.uniqueCheck(user.getWorld(), stack);
            tickGunComponent(stack, leverAction, stack == main);
        }
    }

    private void tickGunComponent(ItemStack stack, LeverActionGunItem leverAction, boolean isSelected) {
        IItemComponent.execute(leverAction.getGunComponent(), stack, gunComponent -> {
            var uuid = UniqueComponent.getOrSet(stack);

            var animationContext = leverAction.createAnimationContext(user.getWorld(), uuid);
            LeverActionPlaySoundContext playSoundContext = leverAction.createPlaySoundContext(user.getWorld(), user);

            boolean markDuty = false;

            if (gunComponent.tick(
                    playSoundContext,
                    leverAction.createCycleTickContext(),
                    leverAction.createReloadTickContext(user, getInventory().orElse(null)),
                    1f / 20f,
                    isSelected
            )) {
                markDuty = true;
            }

            if (!isSelected) {
                if (markDuty) {
                    return IItemComponent.ComponentResult.MODIFIED;
                } else {
                    return IItemComponent.ComponentResult.NO_CHANGE;
                }
            }

            // FIREキー（射撃操作）
            if (keyInputManager.isTurnPressWithin(KeyInputManager.Key.FIRE, 2)) {
                if (gunComponent.canTrigger()) {
                    var fireStartContext = leverAction.createFireStartContext(user.getWorld(), user);
                    if (gunComponent.trigger(playSoundContext, animationContext, fireStartContext)) {
                        stack.damage(1, user, p -> p.sendToolBreakStatus(user.getActiveHand()));
                        keyInputManager.killTurnPressWithin(KeyInputManager.Key.FIRE, 2);
                        markDuty = true;
                    }
                }
            }

            // COCKキー（サイクル操作）
            if (keyInputManager.isTurnPressWithin(KeyInputManager.Key.COCK, 4)) {
                if (gunComponent.canCycle()) {
                    if (gunComponent.cycle(playSoundContext, animationContext)) {
                        keyInputManager.killTurnPressWithin(KeyInputManager.Key.COCK, 4);
                        markDuty = true;
                    }
                }
            }

            // RELOADキー（リロード操作）
            if (keyInputManager.isTurnPressWithin(KeyInputManager.Key.RELOAD, 2)) {
                var isAiming = user instanceof HasAimManager hasAimManager
                        && hasAimManager.actionArms$getAimManager().isAiming();
                Reloadable.ReloadStartContext reloadContext = leverAction.createReloadStartContext(user);
                if (!isAiming && gunComponent.canReload(reloadContext)) {
                    if (gunComponent.reload(playSoundContext, animationContext, reloadContext)) {
                        keyInputManager.killTurnPressWithin(KeyInputManager.Key.RELOAD, 2);
                        markDuty = true;
                    }
                }
            }

            if (markDuty) {
                return IItemComponent.ComponentResult.MODIFIED;
            } else {
                return IItemComponent.ComponentResult.NO_CHANGE;
            }
        });
    }

    protected Optional<Inventory> getInventory() {
        if (this.user instanceof PlayerEntity) {
            return Optional.of(((PlayerEntity) this.user).getInventory());
        }
        return Optional.empty();
    }
}
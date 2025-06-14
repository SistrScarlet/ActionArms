package net.sistr.actionarms.entity.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.ItemUniqueManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.IItemComponent;
import net.sistr.actionarms.item.component.LeverActionPlaySoundContext;
import net.sistr.actionarms.item.component.Reloadable;
import net.sistr.actionarms.item.component.UniqueComponent;

import java.util.List;
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
                return;
            }
            ItemUniqueManager.INSTANCE.uniqueCheck(user.getWorld(), stack);
            tickGunComponent(stack, leverAction, stack == main);
        }
    }

    private void tickGunComponent(ItemStack stack, LeverActionGunItem leverAction, boolean isSelected) {
        IItemComponent.execute(leverAction.getGunComponent(), stack, gunComponent -> {
            var uuid = UniqueComponent.get(stack);

            var animationContext = leverAction.createAnimationContext(user.getWorld(), uuid);
            LeverActionPlaySoundContext playSoundContext = leverAction.createPlaySoundContext(user.getWorld(), user);

            boolean markDuty = false;

            if (gunComponent.tick(
                    playSoundContext,
                    leverAction.createCycleTickContext(),
                    leverAction.createReloadTickContext(user),
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
            if (keyInputManager.isTurnPress(KeyInputManager.Key.FIRE)) {
                if (gunComponent.canTrigger()) {
                    var fireStartContext = leverAction.createFireStartContext(user.getWorld(), user, uuid);
                    if (gunComponent.trigger(playSoundContext, animationContext, fireStartContext)) {
                        markDuty = true;
                    }
                }
            }

            // COCKキー（サイクル操作）
            if (keyInputManager.isTurnPress(KeyInputManager.Key.COCK)) {
                if (gunComponent.canCycle()) {
                    if (gunComponent.cycle(playSoundContext, animationContext)) {
                        markDuty = true;
                    }
                }
            }

            // RELOADキー（リロード操作）
            if (keyInputManager.isTurnPress(KeyInputManager.Key.RELOAD)) {
                Reloadable.ReloadStartContext reloadContext = leverAction.createReloadStartContext(user);
                if (gunComponent.canReload(reloadContext)) {
                    if (gunComponent.reload(playSoundContext, animationContext, reloadContext)) {
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
}
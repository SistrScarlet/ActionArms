package net.sistr.actionarms.entity.util;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.ItemUniqueManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.IComponent;
import net.sistr.actionarms.item.util.LeverActionPlaySoundContext;
import net.sistr.actionarms.item.util.Reloadable;

public class GunController {
    private final LivingEntity user;
    private final IKeyInputManager keyInputManager;
    private final Supplier<List<ItemStack>> itemsSupplier;

    public GunController(
            LivingEntity user,
            IKeyInputManager keyInputManager,
            Supplier<List<ItemStack>> itemsSupplier) {
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

    private void tickGunComponent(
            ItemStack stack, LeverActionGunItem leverAction, boolean isSelected) {
        IComponent.execute(
                leverAction.getGunComponent(),
                stack,
                gunComponent -> {
                    var uuid = ItemUniqueManager.INSTANCE.getOrSet(stack);

                    var animationContext =
                            leverAction.createAnimationContext(user.getWorld(), uuid);
                    LeverActionPlaySoundContext playSoundContext =
                            leverAction.createPlaySoundContext(user.getWorld(), user);

                    boolean markDuty =
                            gunComponent.tick(
                                    playSoundContext,
                                    leverAction.createCycleTickContext(),
                                    leverAction.createReloadTickContext(
                                            user, getInventory().orElse(null)),
                                    1f / 20f,
                                    isSelected);

                    if (!isSelected) {
                        return markDuty
                                ? IComponent.ComponentResult.MODIFIED
                                : IComponent.ComponentResult.NO_CHANGE;
                    }

                    // FIREキー（射撃操作）
                    if (tryKeyAction(KeyInputManager.Key.FIRE, 2, gunComponent::canTrigger)) {
                        var fireStartContext =
                                leverAction.createFireStartContext(user.getWorld(), user);
                        if (gunComponent.trigger(
                                playSoundContext, animationContext, fireStartContext)) {
                            stack.damage(1, user, p -> p.sendToolBreakStatus(user.getActiveHand()));
                            markDuty = true;
                        }
                    }

                    // COCKキー（サイクル操作）
                    if (tryKeyAction(KeyInputManager.Key.COCK, 4, gunComponent::canCycle)) {
                        if (gunComponent.cycle(playSoundContext, animationContext)) {
                            markDuty = true;
                        }
                    }

                    // RELOADキー（リロード操作）
                    Reloadable.ReloadStartContext reloadContext =
                            leverAction.createReloadStartContext(user);
                    boolean isAiming =
                            user instanceof HasAimManager hasAimManager
                                    && hasAimManager.actionArms$getAimManager().isAiming();
                    if (!isAiming
                            && tryKeyAction(
                                    KeyInputManager.Key.RELOAD,
                                    2,
                                    () -> gunComponent.canReload(reloadContext))) {
                        if (gunComponent.reload(
                                playSoundContext, animationContext, reloadContext)) {
                            markDuty = true;
                        }
                    }

                    return markDuty
                            ? IComponent.ComponentResult.MODIFIED
                            : IComponent.ComponentResult.NO_CHANGE;
                });
    }

    /** キー入力を確認し、条件を満たせばキー入力を消費して true を返す。 */
    private boolean tryKeyAction(KeyInputManager.Key key, int window, BooleanSupplier canAction) {
        if (!keyInputManager.isTurnPressWithin(key, window)) {
            return false;
        }
        if (!canAction.getAsBoolean()) {
            return false;
        }
        keyInputManager.killTurnPressWithin(key, window);
        return true;
    }

    protected Optional<Inventory> getInventory() {
        if (this.user instanceof PlayerEntity) {
            return Optional.of(((PlayerEntity) this.user).getInventory());
        }
        return Optional.empty();
    }
}

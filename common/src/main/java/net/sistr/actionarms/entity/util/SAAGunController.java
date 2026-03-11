package net.sistr.actionarms.entity.util;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.ItemUniqueManager;
import net.sistr.actionarms.item.SAAGunItem;
import net.sistr.actionarms.item.component.IComponent;
import net.sistr.actionarms.item.component.SAAGunComponent;

public class SAAGunController {
    private final LivingEntity user;
    private final IKeyInputManager keyInputManager;
    private final Supplier<List<ItemStack>> itemsSupplier;

    public SAAGunController(
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
            if (!(stack.getItem() instanceof SAAGunItem saaItem)) {
                continue;
            }
            ItemUniqueManager.INSTANCE.uniqueCheck(user.getWorld(), stack);
            tickGunComponent(stack, saaItem, stack == main);
        }
    }

    private void tickGunComponent(ItemStack stack, SAAGunItem saaItem, boolean isSelected) {
        IComponent.execute(
                saaItem.getGunComponent(),
                stack,
                gunComponent -> {
                    var uuid = ItemUniqueManager.INSTANCE.getOrSet(stack);
                    var soundContext = saaItem.createSoundContext(user.getWorld(), user);
                    var animContext = saaItem.createAnimationContext(user.getWorld(), uuid);

                    boolean markDuty =
                            gunComponent.tick(soundContext, 1f / 20f, isSelected);

                    if (!isSelected) {
                        return markDuty
                                ? IComponent.ComponentResult.MODIFIED
                                : IComponent.ComponentResult.NO_CHANGE;
                    }

                    // ファニング: トリガー保持中 + コック完了 → 即射撃
                    if (keyInputManager.isPress(KeyInputManager.Key.FIRE)
                            && gunComponent.isHammerCocked()) {
                        var fireContext = saaItem.createFireContext(user.getWorld(), user);
                        if (gunComponent.pullTrigger(soundContext, animContext, fireContext)) {
                            stack.damage(1, user, p -> p.sendToolBreakStatus(user.getActiveHand()));
                            markDuty = true;
                        }
                    }

                    if (gunComponent.isGateOpen()) {
                        // ゲート開放中の操作

                        // FIRE キー → ゲート閉じる
                        if (tryKeyAction(
                                KeyInputManager.Key.FIRE,
                                2,
                                () -> gunComponent.getPhase() == SAAGunComponent.Phase.GATE_OPEN)) {
                            gunComponent.closeGate();
                            soundContext.playSound("GATE_CLOSE");
                            markDuty = true;
                        }

                        // COCK キー → 排莢（空でも実行可、空なら何も起きない）
                        if (tryKeyAction(
                                KeyInputManager.Key.COCK, 4, gunComponent::canEjectAtGate)) {
                            if (gunComponent.ejectAtGate(soundContext, animContext)) {
                                markDuty = true;
                            }
                        }

                        // RELOAD キー → 排莢 / 装填 / シリンダー回転
                        if (tryKeyAction(
                                KeyInputManager.Key.RELOAD,
                                2,
                                () ->
                                        gunComponent.getPhase() == SAAGunComponent.Phase.GATE_OPEN
                                                && gunComponent
                                                        .getCylinder()
                                                        .shouldEjectAtGate())) {
                            if (gunComponent.ejectAtGate(soundContext, animContext)) {
                                markDuty = true;
                            }
                        } else if (tryKeyAction(
                                KeyInputManager.Key.RELOAD,
                                2,
                                () -> gunComponent.canLoadAtGate() && hasBullet())) {
                            var inventory = getInventory();
                            if (inventory.isPresent()) {
                                var bullets =
                                        InventoryAmmoUtil.popBullets(
                                                inventory.get(), bullet -> true, 1);
                                if (!bullets.isEmpty()) {
                                    gunComponent.loadAtGate(bullets.get(0));
                                    markDuty = true;
                                }
                            }
                        } else if (tryKeyAction(
                                KeyInputManager.Key.RELOAD,
                                2,
                                () ->
                                        gunComponent.getPhase() == SAAGunComponent.Phase.GATE_OPEN
                                                && !gunComponent.canLoadAtGate()
                                                && hasBullet())) {
                            // 装填済み薬室をスキップする回転（弾がある場合のみ）
                            gunComponent.getCylinder().loadRotate();
                            markDuty = true;
                        }

                        // OPERATE キー → ゲート閉じる
                        if (tryKeyAction(
                                KeyInputManager.Key.OPERATE,
                                2,
                                () -> gunComponent.getPhase() == SAAGunComponent.Phase.GATE_OPEN)) {
                            gunComponent.closeGate();
                            soundContext.playSound("GATE_CLOSE");
                            markDuty = true;
                        }
                    } else {
                        // ゲート閉じ中の操作

                        // FIRE キー → 射撃（ファニング以外の通常射撃）
                        if (tryKeyAction(
                                KeyInputManager.Key.FIRE, 2, gunComponent::canPullTrigger)) {
                            var fireContext = saaItem.createFireContext(user.getWorld(), user);
                            if (gunComponent.pullTrigger(soundContext, animContext, fireContext)) {
                                stack.damage(
                                        1, user, p -> p.sendToolBreakStatus(user.getActiveHand()));
                                markDuty = true;
                            }
                        }

                        // COCK キー → コック
                        if (tryKeyAction(
                                KeyInputManager.Key.COCK, 4, gunComponent::canCockHammer)) {
                            if (gunComponent.cockHammer(soundContext, animContext)) {
                                markDuty = true;
                            }
                        }

                        // OPERATE キー → ゲート開く
                        if (tryKeyAction(
                                KeyInputManager.Key.OPERATE, 2, () -> !gunComponent.isGateOpen())) {
                            gunComponent.openGate();
                            soundContext.playSound("GATE_OPEN");
                            markDuty = true;
                        }
                    }

                    return markDuty
                            ? IComponent.ComponentResult.MODIFIED
                            : IComponent.ComponentResult.NO_CHANGE;
                });
    }

    private boolean tryKeyAction(
            KeyInputManager.Key key, int window, java.util.function.BooleanSupplier canAction) {
        if (!keyInputManager.isTurnPressWithin(key, window)) {
            return false;
        }
        if (!canAction.getAsBoolean()) {
            return false;
        }
        keyInputManager.killTurnPressWithin(key, window);
        return true;
    }

    private boolean hasBullet() {
        if (this.user instanceof PlayerEntity player) {
            return InventoryAmmoUtil.hasBullet(player.getInventory(), bullet -> true);
        }
        return false;
    }

    protected Optional<Inventory> getInventory() {
        if (this.user instanceof PlayerEntity) {
            return Optional.of(((PlayerEntity) this.user).getInventory());
        }
        return Optional.empty();
    }
}

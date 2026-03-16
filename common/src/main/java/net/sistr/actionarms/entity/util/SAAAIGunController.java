package net.sistr.actionarms.entity.util;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.ItemUniqueManager;
import net.sistr.actionarms.item.SAAGunItem;
import net.sistr.actionarms.item.component.IComponent;
import net.sistr.actionarms.item.component.SAAGunComponent;
import net.sistr.actionarms.item.util.AnimationContext;

public class SAAAIGunController implements AIGunController {
    private static final int FIRE_COOLDOWN = 5;
    private static final int COCK_COOLDOWN = 3;
    private static final int GATE_COOLDOWN = 3;
    private static final int EJECT_COOLDOWN = 2;
    private static final int LOAD_COOLDOWN = 2;

    private final LivingEntity user;
    private final SAAGunItem gunItem;
    private final Supplier<ItemStack> stackSupplier;
    private final Supplier<Optional<Inventory>> inventorySupplier;
    private GunGoal goal = GunGoal.IDLE;
    private float cooldownMultiplier = 1f;
    private int operationCooldown;

    public SAAAIGunController(
            LivingEntity user,
            SAAGunItem gunItem,
            Supplier<ItemStack> stackSupplier,
            Supplier<Optional<Inventory>> inventorySupplier) {
        this.user = user;
        this.gunItem = gunItem;
        this.stackSupplier = stackSupplier;
        this.inventorySupplier = inventorySupplier;
    }

    @Override
    public void setGoal(GunGoal goal) {
        this.goal = goal;
    }

    @Override
    public GunGoal getGoal() {
        return goal;
    }

    @Override
    public void setCooldownMultiplier(float multiplier) {
        this.cooldownMultiplier = multiplier;
    }

    @Override
    public GunStatus getStatus() {
        var stack = stackSupplier.get();
        var component = IComponent.query(gunItem.getGunComponent(), stack, c -> c);
        boolean hasAmmo = hasBulletInInventory();

        boolean hasShootable = hasShootableChamber(component);
        boolean canAttack = hasShootable || hasAmmo;
        boolean canReload = !component.getCylinder().isAllReady() && hasAmmo;

        return new GunStatus(canAttack, canReload);
    }

    @Override
    public TickResult tick(float timeDelta) {
        if (user.getWorld().isClient) {
            return new TickResult(false);
        }

        var stack = stackSupplier.get();
        if (!(stack.getItem() instanceof SAAGunItem)) {
            return new TickResult(false);
        }

        ItemUniqueManager.INSTANCE.uniqueCheck(user.getWorld(), stack);

        if (operationCooldown > 0) {
            operationCooldown--;
        }

        boolean[] fired = {false};

        IComponent.execute(
                gunItem.getGunComponent(),
                stack,
                component -> {
                    var uuid = ItemUniqueManager.INSTANCE.getOrSet(stack);
                    var soundContext = gunItem.createSoundContext(user.getWorld(), user);
                    var animContext = gunItem.createAnimationContext(user.getWorld(), uuid);

                    boolean markDuty = component.tick(soundContext, timeDelta, true);

                    if (goal == GunGoal.IDLE || operationCooldown > 0) {
                        return markDuty
                                ? IComponent.ComponentResult.MODIFIED
                                : IComponent.ComponentResult.NO_CHANGE;
                    }

                    if (component.isGateOpen()) {
                        markDuty |= tickGateOpen(component, soundContext, animContext);
                    } else {
                        markDuty |= tickGateClosed(component, soundContext, animContext, fired);
                    }

                    return markDuty
                            ? IComponent.ComponentResult.MODIFIED
                            : IComponent.ComponentResult.NO_CHANGE;
                });

        return new TickResult(fired[0]);
    }

    private boolean tickGateOpen(
            SAAGunComponent component,
            SAAGunComponent.SoundContext soundContext,
            AnimationContext animContext) {
        if (component.getPhase() != SAAGunComponent.Phase.GATE_OPEN) {
            return false;
        }

        // 排莢が必要なら排莢
        if (component.getCylinder().shouldEjectAtGate()) {
            if (component.ejectAtGate(soundContext, animContext)) {
                applyOperationCooldown(EJECT_COOLDOWN);
                return true;
            }
        }

        // 装填可能なら装填
        if (component.canLoadAtGate()) {
            var inventory = inventorySupplier.get();
            if (inventory.isPresent()) {
                var bullets = InventoryAmmoUtil.popBullets(inventory.get(), b -> true, 1);
                if (!bullets.isEmpty()) {
                    component.loadAtGate(bullets.get(0));
                    applyOperationCooldown(LOAD_COOLDOWN);
                    return true;
                }
            }
        }

        // 他に処理すべき薬室があればスマート回転
        if (!component.getCylinder().isAllReady() && hasBulletInInventory()) {
            component.smartGateRotate();
            return true;
        }

        // これ以上できることがない → ゲートを閉じる
        component.closeGate();
        soundContext.playSound("GATE_CLOSE");
        applyOperationCooldown(GATE_COOLDOWN);
        return true;
    }

    private boolean tickGateClosed(
            SAAGunComponent component,
            SAAGunComponent.SoundContext soundContext,
            AnimationContext animContext,
            boolean[] fired) {
        boolean shouldAttack = goal == GunGoal.ATTACK;
        boolean shouldReady = shouldAttack || goal == GunGoal.READY;
        boolean needsReload =
                goal == GunGoal.RELOAD || (shouldReady && !hasShootableChamber(component));

        // 射撃
        if (shouldAttack
                && component.canPullTrigger()
                && component.getCylinder().canShootFiring()) {
            var fireContext = gunItem.createFireContext(user.getWorld(), user);
            if (component.pullTrigger(soundContext, animContext, fireContext)) {
                var stack = stackSupplier.get();
                stack.damage(1, user, p -> p.sendToolBreakStatus(user.getActiveHand()));
                fired[0] = true;
                applyOperationCooldown(FIRE_COOLDOWN);
                return true;
            }
        }

        // コック（撃てる弾がある場合のみ）
        if (shouldReady && component.canCockHammer() && hasShootableChamber(component)) {
            if (component.cockHammer(soundContext, animContext)) {
                applyOperationCooldown(COCK_COOLDOWN);
                return true;
            }
        }

        // リロードが必要 → ゲート開放
        if (needsReload
                && !component.getCylinder().isAllReady()
                && hasBulletInInventory()
                && component.getPhase() == SAAGunComponent.Phase.IDLE) {
            component.openGate();
            soundContext.playSound("GATE_OPEN");
            applyOperationCooldown(GATE_COOLDOWN);
            return true;
        }

        return false;
    }

    private void applyOperationCooldown(int baseTicks) {
        operationCooldown = Math.round(baseTicks * cooldownMultiplier);
    }

    private boolean hasShootableChamber(SAAGunComponent component) {
        var cylinder = component.getCylinder();
        for (int i = 0; i < cylinder.getCapacity(); i++) {
            if (cylinder.getChamberAt(i).canShoot()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBulletInInventory() {
        return inventorySupplier
                .get()
                .map(inv -> InventoryAmmoUtil.hasBullet(inv, b -> true))
                .orElse(false);
    }
}

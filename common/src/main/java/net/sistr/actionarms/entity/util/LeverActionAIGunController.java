package net.sistr.actionarms.entity.util;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.ItemUniqueManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.IComponent;
import net.sistr.actionarms.item.component.LeverActionGunComponent;
import net.sistr.actionarms.item.util.Reloadable;

public class LeverActionAIGunController implements AIGunController {
    private static final int FIRE_COOLDOWN = 5;
    private static final int CYCLE_COOLDOWN = 3;
    private static final int RELOAD_COOLDOWN = 3;

    private final LivingEntity user;
    private final LeverActionGunItem gunItem;
    private final Supplier<ItemStack> stackSupplier;
    private final Supplier<Optional<Inventory>> inventorySupplier;
    private GunGoal goal = GunGoal.IDLE;
    private float cooldownMultiplier = 1f;
    private int operationCooldown;

    public LeverActionAIGunController(
            LivingEntity user,
            LeverActionGunItem gunItem,
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
        boolean hasAmmo = hasInventoryAmmo(component);

        boolean canAttack =
                component.getChamber().canShoot() || component.getMagazine().hasBullet() || hasAmmo;
        boolean canReload = component.getMagazine().canAddBullet() && hasAmmo;

        return new GunStatus(canAttack, canReload);
    }

    @Override
    public TickResult tick(float timeDelta) {
        if (user.getWorld().isClient) {
            return new TickResult(false);
        }

        var stack = stackSupplier.get();
        if (!(stack.getItem() instanceof LeverActionGunItem)) {
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
                    var soundContext = gunItem.createPlaySoundContext(user.getWorld(), user);
                    var animContext = gunItem.createAnimationContext(user.getWorld(), uuid);
                    var cycleTickContext = gunItem.createCycleTickContext();
                    var reloadTickContext =
                            gunItem.createReloadTickContext(
                                    user, inventorySupplier.get().orElse(null));

                    boolean markDuty =
                            component.tick(
                                    soundContext,
                                    cycleTickContext,
                                    reloadTickContext,
                                    timeDelta,
                                    true);

                    if (goal == GunGoal.IDLE || operationCooldown > 0) {
                        return markDuty
                                ? IComponent.ComponentResult.MODIFIED
                                : IComponent.ComponentResult.NO_CHANGE;
                    }

                    boolean shouldAttack = goal == GunGoal.ATTACK;
                    boolean shouldReady = shouldAttack || goal == GunGoal.READY;
                    boolean shouldReload =
                            goal == GunGoal.RELOAD
                                    || (shouldReady && !component.getChamber().canShoot());

                    if (shouldAttack
                            && component.canPullTrigger()
                            && component.getChamber().canShoot()) {
                        var fireContext = gunItem.createFireStartContext(user.getWorld(), user);
                        if (component.pullTrigger(soundContext, animContext, fireContext)) {
                            stack.damage(1, user, p -> p.sendToolBreakStatus(user.getActiveHand()));
                            fired[0] = true;
                            markDuty = true;
                            applyOperationCooldown(FIRE_COOLDOWN);
                        }
                    } else if (shouldReady
                            && component.shouldCycleLever()
                            && component.canCycleLever()) {
                        if (component.cycleLever(soundContext, animContext)) {
                            markDuty = true;
                            applyOperationCooldown(CYCLE_COOLDOWN);
                        }
                    } else if (shouldReload) {
                        Reloadable.ReloadStartContext reloadStartContext =
                                createReloadStartContext(component);
                        if (component.canLoadBullet(reloadStartContext)) {
                            if (component.loadBullet(
                                    soundContext, animContext, reloadStartContext)) {
                                markDuty = true;
                                applyOperationCooldown(RELOAD_COOLDOWN);
                            }
                        }
                    }

                    return markDuty
                            ? IComponent.ComponentResult.MODIFIED
                            : IComponent.ComponentResult.NO_CHANGE;
                });

        return new TickResult(fired[0]);
    }

    private void applyOperationCooldown(int baseTicks) {
        operationCooldown = Math.round(baseTicks * cooldownMultiplier);
    }

    private Reloadable.ReloadStartContext createReloadStartContext(
            LeverActionGunComponent component) {
        return predicate ->
                inventorySupplier
                        .get()
                        .map(inv -> InventoryAmmoUtil.hasBullet(inv, predicate))
                        .orElse(false);
    }

    private boolean hasInventoryAmmo(LeverActionGunComponent component) {
        return inventorySupplier
                .get()
                .map(
                        inv ->
                                InventoryAmmoUtil.hasBullet(
                                        inv,
                                        component.getMagazine().getMagazineType().allowBullet()))
                .orElse(false);
    }
}

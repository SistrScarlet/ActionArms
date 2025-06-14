package net.sistr.actionarms.entity.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.UniqueComponent;
import org.jetbrains.annotations.Nullable;

public class AimManager implements IAimManager {
    private final PlayerEntity player;
    private boolean aiming;
    @Nullable
    private ItemStack prevAimStack;

    public AimManager(PlayerEntity player) {
        this.player = player;
    }

    public void tick() {
        var stack = this.player.getMainHandStack();

        if (!canAiming()) {
            aiming = false;
            prevAimStack = stack;
            return;
        }
        prevAimStack = stack;
    }

    private boolean canAiming() {
        var stack = this.player.getMainHandStack();
        // レバアク以外だったら不可
        if (!(stack.getItem() instanceof LeverActionGunItem)) {
            return false;
        }
        // アイテムを切り替えたらエイム解除
        if (prevAimStack != null && prevAimStack != stack) {
            var prevUuid = UniqueComponent.get(prevAimStack);
            var uuid = UniqueComponent.get(stack);
            return prevUuid.equals(uuid);
        }

        return true;
    }

    @Override
    public boolean isAiming() {
        return this.aiming;
    }

    @Override
    public void setAiming(boolean aim) {
        this.aiming = aim && canAiming();
    }
}

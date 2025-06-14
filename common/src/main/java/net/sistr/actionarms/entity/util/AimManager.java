package net.sistr.actionarms.entity.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.LeverActionGunItem;

public class AimManager implements IAimManager {
    private final PlayerEntity player;
    private boolean aiming;
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

        // アイテムを切り替えたらエイム解除
        if (prevAimStack != stack) {
            return false;
        }
        // レバアク以外だったら不可
        if (!(stack.getItem() instanceof LeverActionGunItem)) {
            return false;
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

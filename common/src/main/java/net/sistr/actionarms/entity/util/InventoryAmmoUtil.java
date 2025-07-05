package net.sistr.actionarms.entity.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.BulletItem;
import net.sistr.actionarms.item.component.BulletComponent;
import net.sistr.actionarms.item.component.IItemComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class InventoryAmmoUtil {

    public static List<BulletComponent> popBullets(Inventory inventory, Predicate<BulletComponent> predicate, int limit) {
        var bullets = new ArrayList<BulletComponent>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            int finalI = i;
            getBullet(stack)
                    .filter(predicate)
                    .ifPresent(bullet -> {
                        int remain = limit - bullets.size();
                        var stacks = inventory.removeStack(finalI, remain);
                        for (int j = 0; j < stacks.getCount(); j++) {
                            bullets.add(bullet.copy());
                        }
                    });
            if (bullets.size() >= limit) {
                return bullets;
            }
        }
        return bullets;
    }

    public static List<BulletComponent> getBullets(Inventory inventory, Predicate<BulletComponent> predicate) {
        var bullets = new ArrayList<BulletComponent>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            getBullet(stack)
                    .filter(predicate)
                    .ifPresent(bullets::add);
        }
        return bullets;
    }

    public static boolean hasBullet(Inventory inventory, Predicate<BulletComponent> predicate) {
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (getBullet(stack)
                    .filter(predicate)
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<BulletComponent> getBullet(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BulletItem bulletItem)) {
            return Optional.empty();
        }
        var bulletComponent = IItemComponent.query(bulletItem.getComponentSupplier(), stack, c -> c);
        return Optional.ofNullable(bulletComponent);
    }
}

package net.sistr.actionarms.entity.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.BulletItem;
import net.sistr.actionarms.item.component.BulletDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class InventoryAmmoUtil {

    public static List<BulletDataType> popBullets(Inventory inventory, Predicate<BulletDataType> predicate, int limit) {
        var bullets = new ArrayList<BulletDataType>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            int finalI = i;
            getBullet(stack)
                    .filter(predicate)
                    .ifPresent(bullet -> {
                        int remain = limit - bullets.size();
                        var stacks = inventory.removeStack(finalI, remain);
                        for (int j = 0; j < stacks.getCount(); j++) {
                            bullets.add(bullet);
                        }
                    });
            if (bullets.size() >= limit) {
                return bullets;
            }
        }
        return bullets;
    }

    public static List<BulletDataType> getBullets(Inventory inventory, Predicate<BulletDataType> predicate) {
        var bullets = new ArrayList<BulletDataType>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            getBullet(stack)
                    .filter(predicate)
                    .ifPresent(bullets::add);
        }
        return bullets;
    }

    public static boolean hasBullet(Inventory inventory, Predicate<BulletDataType> predicate) {
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

    public static Optional<BulletDataType> getBullet(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BulletItem bulletItem)) {
            return Optional.empty();
        }
        return Optional.ofNullable(bulletItem.getComponentSupplier().get());
    }
}

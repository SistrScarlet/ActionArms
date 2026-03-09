package net.sistr.actionarms.entity.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.BulletItem;
import net.sistr.actionarms.item.data.BulletData;

public class InventoryAmmoUtil {

    public static List<BulletData> popBullets(
            Inventory inventory, Predicate<BulletData> predicate, int limit) {
        var bullets = new ArrayList<BulletData>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            int finalI = i;
            getBullet(stack)
                    .filter(predicate)
                    .ifPresent(
                            bullet -> {
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

    public static List<BulletData> getBullets(
            Inventory inventory, Predicate<BulletData> predicate) {
        var bullets = new ArrayList<BulletData>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            getBullet(stack).filter(predicate).ifPresent(bullets::add);
        }
        return bullets;
    }

    public static boolean hasBullet(Inventory inventory, Predicate<BulletData> predicate) {
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (getBullet(stack).filter(predicate).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<BulletData> getBullet(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BulletItem bulletItem)) {
            return Optional.empty();
        }
        return Optional.ofNullable(bulletItem.getBulletData());
    }
}

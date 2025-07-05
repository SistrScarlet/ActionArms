package net.sistr.actionarms.item;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.sistr.actionarms.item.component.IItemComponent;
import net.sistr.actionarms.item.component.UniqueComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemUniqueManager {
    public static final ItemUniqueManager INSTANCE = new ItemUniqueManager();
    private final Map<UUID, Long> stateMap = new HashMap<>();

    public void clearOld(World world) {
        stateMap.values()
                .removeIf(worldTime -> worldTime < world.getTime() - 5);
    }

    public void uniqueCheck(World world, ItemStack stack) {
        if (world.isClient) {
            return;
        }
        long worldTime = world.getTime();
        var uuid = UniqueComponent.getOrSet(stack);
        var savedWorldTime = stateMap.get(uuid);
        // 重複した状態が存在している
        if (savedWorldTime != null && savedWorldTime.equals(worldTime)) {
            // 対象のUUIDを変更する
            IItemComponent.update(UniqueComponent::new, stack, UniqueComponent::reset);
            uniqueCheck(world, stack);
            return;
        }
        stateMap.put(uuid, worldTime);
    }

}

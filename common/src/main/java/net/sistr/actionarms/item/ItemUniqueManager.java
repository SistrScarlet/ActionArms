package net.sistr.actionarms.item;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ItemUniqueManager {
    public static final ItemUniqueManager INSTANCE = new ItemUniqueManager();
    private final Map<UUID, Long> stateMap = new HashMap<>();

    public Optional<UUID> getUUID(ItemStack stack) {
        if (stack.getNbt() == null) return Optional.empty();
        var nbt = stack.getNbt();
        if (!nbt.containsUuid("uniqueId")) return Optional.empty();
        var uuid = nbt.getUuid("uniqueId");
        return Optional.of(uuid);
    }

    public UUID getOrSet(ItemStack stack) {
        // queryだと値がセットされない
        var nbt = stack.getOrCreateNbt();
        if (!nbt.containsUuid("uniqueId")) {
            var uuid = UUID.randomUUID();
            nbt.putUuid("uniqueId", uuid);
            return uuid;
        }
        return nbt.getUuid("uniqueId");
    }

    public void setUUID(ItemStack stack, UUID uuid) {
        var nbt = stack.getOrCreateNbt();
        nbt.putUuid("uniqueId", uuid);
    }

    public void clearOld(World world) {
        stateMap.values()
                .removeIf(worldTime -> worldTime < world.getTime() - 5);
    }

    public void uniqueCheck(World world, ItemStack stack) {
        if (world.isClient) {
            return;
        }
        long worldTime = world.getTime();
        var uuid = getOrSet(stack);
        var savedWorldTime = stateMap.get(uuid);
        // 重複した状態が存在している
        if (Long.valueOf(worldTime).equals(savedWorldTime)) {
            // 対象のUUIDを変更する
            setUUID(stack, UUID.randomUUID());
            uniqueCheck(world, stack);
            return;
        }
        stateMap.put(uuid, worldTime);
    }

}

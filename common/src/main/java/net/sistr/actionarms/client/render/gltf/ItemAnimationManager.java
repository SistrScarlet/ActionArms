package net.sistr.actionarms.client.render.gltf;

import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.component.UniqueComponent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemAnimationManager {
    public static final ItemAnimationManager INSTANCE = new ItemAnimationManager();
    private final Map<UUID, Map<String, State>> itemStateMap = new HashMap<>();

    public void tick(float timeDelta) {
        for (Map<String, State> states : itemStateMap.values()) {
            states.replaceAll((k, v) -> new State(k, v.seconds() + timeDelta));
            states.values().removeIf(state -> state.seconds() > 10);
        }
    }

    public void setAnimation(@Nullable UUID uuid, @Nullable String animationId, float seconds) {
        if (uuid != null && animationId != null) {
            itemStateMap.computeIfAbsent(uuid, i -> new HashMap<>()).put(animationId, new State(animationId, seconds));
        }
    }

    public Map<String, State> getItemStateMap(@Nullable UUID uuid) {
        return uuid != null ? itemStateMap.getOrDefault(uuid, new HashMap<>()) : new HashMap<>();
    }

    public Map<String, State> getItemStateMap(@Nullable ItemStack stack) {
        if (stack == null) {
            return new HashMap<>();
        }
        var uuid = UniqueComponent.getOrSet(stack);
        return getItemStateMap(uuid);
    }

    public record State(String id, float seconds) {

    }

}

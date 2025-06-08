package net.sistr.actionarms.client.render.gltf;

import net.minecraft.item.ItemStack;
import net.sistr.actionarms.item.component.UniqueComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemAnimationManager {
    public static final ItemAnimationManager INSTANCE = new ItemAnimationManager();
    private final Map<UUID, Map<String, State>> itemStateMap = new HashMap<>();

    public void tick(float timeDelta) {
        for (Map<String, State> states : itemStateMap.values()) {
            states.replaceAll((k, v) -> new State(k, v.seconds + timeDelta));
            states.values().removeIf(state -> state.seconds > 10);
        }
    }

    public void setAnimation(UUID uuid, String animationId, float seconds) {
        itemStateMap.computeIfAbsent(uuid, i -> new HashMap<>()).put(animationId, new State(animationId, seconds));
    }

    public Map<String, State> getItemStateMap(UUID uuid) {
        return itemStateMap.getOrDefault(uuid, new HashMap<>());
    }

    public Map<String, State> getItemStateMap(ItemStack stack) {
        var uuid = UniqueComponent.get(stack);
        return getItemStateMap(uuid);
    }

    public record State(String id, float seconds) {

    }

}

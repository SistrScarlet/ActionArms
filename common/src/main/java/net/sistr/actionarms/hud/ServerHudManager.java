package net.sistr.actionarms.hud;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.IItemComponent;
import net.sistr.actionarms.item.component.UniqueComponent;
import net.sistr.actionarms.network.HudStatePacket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class ServerHudManager {
    public static final ServerHudManager INSTANCE = new ServerHudManager();
    private final Map<UUID, Map<String, HudState<?>>> hudStateMap = new HashMap<>();

    public void tick(ServerWorld world) {
        var uuidSet = world.getPlayers().stream()
                .map(ServerPlayerEntity::getUuid)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        hudStateMap.keySet().removeIf(uuid -> !uuidSet.contains(uuid));
        world.getPlayers().forEach(this::updateHud);
        hudStateMap.values().forEach(map -> map.values()
                .removeIf(state -> state.lastUpdateTime < world.getTime() - 20L));
    }

    private void updateHud(ServerPlayerEntity player) {
        var mainStack = player.getMainHandStack();

        if (mainStack.getItem() instanceof LeverActionGunItem leverActionGunItem) {
            var gunComponent = IItemComponent.query(leverActionGunItem.getGunComponent(), mainStack, c -> c);
            var state = LeverActionHudState.of(gunComponent);
            var map = hudStateMap.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
            var uuid = UniqueComponent.getOrSet(mainStack);
            var id = "lever_action@" + uuid;
            var hudState = map.get(id);
            if (hudState == null || !hudState.prevState.equals(state)) {
                HudStatePacket.sendS2C(player, id, state.write());
                map.put(id, new HudState<>(state, player.getWorld().getTime()));
            }
            if (hudState != null && hudState.prevState.equals(state)) {
                map.put(id, new HudState<>(hudState.prevState, player.getWorld().getTime()));
            }
        }
    }

    private record HudState<T>(T prevState, long lastUpdateTime) {
    }

}

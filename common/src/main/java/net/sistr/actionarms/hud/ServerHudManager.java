package net.sistr.actionarms.hud;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sistr.actionarms.item.ItemUniqueManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.SAAGunItem;
import net.sistr.actionarms.item.component.IComponent;
import net.sistr.actionarms.network.HudStatePacket;

public class ServerHudManager {
    public static final ServerHudManager INSTANCE = new ServerHudManager();
    private final Map<UUID, Map<String, HudState<?>>> hudStateMap = new HashMap<>();

    public void tick(ServerWorld world) {
        var uuidSet =
                world.getPlayers().stream()
                        .map(ServerPlayerEntity::getUuid)
                        .collect(HashSet::new, HashSet::add, HashSet::addAll);
        hudStateMap.keySet().removeIf(uuid -> !uuidSet.contains(uuid));
        world.getPlayers().forEach(this::updateHud);
        hudStateMap
                .values()
                .forEach(
                        map ->
                                map.values()
                                        .removeIf(
                                                state ->
                                                        state.lastUpdateTime
                                                                < world.getTime() - 20L));
    }

    private void updateHud(ServerPlayerEntity player) {
        var mainStack = player.getMainHandStack();

        if (mainStack.getItem() instanceof LeverActionGunItem leverActionGunItem) {
            var gunComponent =
                    IComponent.query(leverActionGunItem.getGunComponent(), mainStack, c -> c);
            var state = LeverActionHudState.of(gunComponent);
            var map = hudStateMap.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
            var uuid = ItemUniqueManager.INSTANCE.getOrSet(mainStack);
            var id = "lever_action@" + uuid;
            syncHudState(player, map, id, state, LeverActionHudState::write);
        }

        if (mainStack.getItem() instanceof SAAGunItem saaGunItem) {
            var gunComponent = IComponent.query(saaGunItem.getGunComponent(), mainStack, c -> c);
            var state = SAAHudState.of(gunComponent);
            var map = hudStateMap.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
            var uuid = ItemUniqueManager.INSTANCE.getOrSet(mainStack);
            var id = "saa@" + uuid;
            syncHudState(player, map, id, state, SAAHudState::write);
        }
    }

    private <T> void syncHudState(
            ServerPlayerEntity player,
            Map<String, HudState<?>> map,
            String id,
            T state,
            Function<T, NbtCompound> writer) {
        var hudState = map.get(id);
        if (hudState == null || !hudState.prevState.equals(state)) {
            HudStatePacket.sendS2C(player, id, writer.apply(state));
            map.put(id, new HudState<>(state, player.getWorld().getTime()));
        }
        if (hudState != null && hudState.prevState.equals(state)) {
            map.put(id, new HudState<>(hudState.prevState, player.getWorld().getTime()));
        }
    }

    private record HudState<T>(T prevState, long lastUpdateTime) {}
}

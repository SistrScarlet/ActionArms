package net.sistr.actionarms.client.render.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ClientHudManager {
    public static final ClientHudManager INSTANCE = new ClientHudManager();
    public final Map<String, HudState> hudMap = new HashMap<>();

    public void updateHud(String id, NbtCompound nbt) {
        var hudState = hudMap.computeIfAbsent(id, HudState::new);
        hudState.setNbt(nbt);
        var world = MinecraftClient.getInstance().world;
        long now = world == null ? 0 : world.getTime();
        hudState.setLastUpdateTime(now);
    }

    public void preTick() {
        hudMap.values().removeIf(hudState -> {
            long lastUpdateTime = hudState.getLastUpdateTime();
            var world = MinecraftClient.getInstance().world;
            long now = world == null ? lastUpdateTime : world.getTime();
            return now - lastUpdateTime > 20;
        });
    }

    public Optional<HudState> getRawState(String id) {
        return Optional.ofNullable(hudMap.get(id));
    }

    public <T> Optional<T> getState(String id, Function<NbtCompound, T> factory) {
        var hudState = hudMap.get(id);
        if (hudState == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(factory.apply(hudState.getNbt()));
    }
}

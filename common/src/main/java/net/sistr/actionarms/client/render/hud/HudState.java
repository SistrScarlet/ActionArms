package net.sistr.actionarms.client.render.hud;

import net.minecraft.nbt.NbtCompound;

public class HudState {
    private final String id;
    private NbtCompound nbt;
    private long lastUpdateTime;

    public HudState(String id) {
        this.id = id;
    }

    public void setNbt(NbtCompound nbt) {
        this.nbt = nbt;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getId() {
        return id;
    }

    public NbtCompound getNbt() {
        return nbt;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
}

package net.sistr.actionarms.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.sistr.actionarms.item.component.SAAGunComponent;

public record SAAHudState(
        int firingIndex, List<ChamberState> chamberStates, boolean gateOpen, boolean hammerCocked) {

    public enum ChamberState {
        EMPTY,
        LOADED,
        SPENT;

        public String id() {
            return name().toLowerCase();
        }

        public static ChamberState fromId(String id) {
            try {
                return ChamberState.valueOf(id.toUpperCase());
            } catch (IllegalArgumentException e) {
                return EMPTY;
            }
        }
    }

    public static SAAHudState of(SAAGunComponent component) {
        var cylinder = component.getCylinder();
        int capacity = cylinder.getCapacity();
        List<ChamberState> states = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            var chamber = cylinder.getChamberAt(i);
            if (chamber.isEmpty()) {
                states.add(ChamberState.EMPTY);
            } else if (chamber.canShoot()) {
                states.add(ChamberState.LOADED);
            } else {
                states.add(ChamberState.SPENT);
            }
        }
        return new SAAHudState(
                cylinder.getFiringIndex(), states, component.isGateOpen(),
                component.isHammerCocked());
    }

    public static SAAHudState of(NbtCompound nbt) {
        int firingIndex = nbt.getInt("firingIndex");
        boolean gateOpen = nbt.getBoolean("gateOpen");
        List<ChamberState> states = new ArrayList<>();
        if (nbt.contains("chamberStates")) {
            NbtList list = nbt.getList("chamberStates", 8);
            for (int i = 0; i < list.size(); i++) {
                states.add(ChamberState.fromId(list.getString(i)));
            }
        }
        boolean hammerCocked = nbt.getBoolean("hammerCocked");
        return new SAAHudState(firingIndex, states, gateOpen, hammerCocked);
    }

    public NbtCompound write() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("firingIndex", this.firingIndex);
        nbt.putBoolean("gateOpen", this.gateOpen);
        nbt.putBoolean("hammerCocked", this.hammerCocked);
        NbtList list = new NbtList();
        for (ChamberState state : this.chamberStates) {
            list.add(NbtString.of(state.id()));
        }
        nbt.put("chamberStates", list);
        return nbt;
    }
}

package net.sistr.actionarms.item.util;

import java.util.Optional;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.sistr.actionarms.item.data.BulletData;

public class Cylinder {
    private final Chamber[] chambers;
    private int firingIndex;

    public Cylinder(int capacity) {
        this.chambers = new Chamber[capacity];
        for (int i = 0; i < capacity; i++) {
            this.chambers[i] = new Chamber(null);
        }
        this.firingIndex = 0;
    }

    // === 回転 ===

    /** コック回転（時計回り）。次の薬室を射撃位置に持ってくる。 */
    public void cockRotate() {
        firingIndex = (firingIndex - 1 + chambers.length) % chambers.length;
    }

    /** 装填回転（反時計回り）。次の薬室をゲート位置に持ってくる。 */
    public void loadRotate() {
        firingIndex = (firingIndex + 1) % chambers.length;
    }

    // === 射撃位置の操作 ===

    public Chamber firingChamber() {
        return chambers[firingIndex];
    }

    public boolean canShootFiring() {
        return firingChamber().canShoot();
    }

    public Optional<BulletData> shootFiring() {
        return firingChamber().shoot();
    }

    // === ゲート位置の操作 ===

    /** ゲート位置のインデックス（射撃位置から時計回り+1） */
    public int gateIndex() {
        return (firingIndex + 1) % chambers.length;
    }

    public Chamber gateChamber() {
        return chambers[gateIndex()];
    }

    public boolean isGateEmpty() {
        return gateChamber().isEmpty();
    }

    public boolean shouldEjectAtGate() {
        return gateChamber().shouldEject();
    }

    public Optional<Cartridge> ejectAtGate() {
        return gateChamber().ejectCartridge();
    }

    public boolean loadAtGate(BulletData bullet) {
        return gateChamber().loadCartridge(new Cartridge(bullet));
    }

    // === クエリ ===

    public int countLoaded() {
        int count = 0;
        for (Chamber c : chambers) {
            if (!c.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public boolean isAllLoaded() {
        for (Chamber c : chambers) {
            if (c.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int getCapacity() {
        return chambers.length;
    }

    public int getFiringIndex() {
        return firingIndex;
    }

    public Chamber getChamberAt(int index) {
        return chambers[index];
    }

    // === NBT ===

    public void read(NbtCompound nbt) {
        this.firingIndex = nbt.getInt("firingIndex");
        NbtList list = nbt.getList("chambers", 10);
        for (int i = 0; i < chambers.length && i < list.size(); i++) {
            chambers[i].read(list.getCompound(i));
        }
    }

    public void write(NbtCompound nbt) {
        nbt.putInt("firingIndex", this.firingIndex);
        NbtList list = new NbtList();
        for (Chamber chamber : chambers) {
            NbtCompound chamberNbt = new NbtCompound();
            chamber.write(chamberNbt);
            list.add(chamberNbt);
        }
        nbt.put("chambers", list);
    }
}

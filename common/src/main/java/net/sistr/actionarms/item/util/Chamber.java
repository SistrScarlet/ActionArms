package net.sistr.actionarms.item.util;

import java.util.Optional;
import net.minecraft.nbt.NbtCompound;
import net.sistr.actionarms.item.data.BulletData;
import org.jetbrains.annotations.Nullable;

public class Chamber {
    @Nullable private Cartridge cartridge;

    public Chamber(@Nullable Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    public Optional<Cartridge> getCartridge() {
        return Optional.ofNullable(cartridge);
    }

    /** 薬莢を装填する。既に装填済みなら false を返す。 */
    public boolean loadCartridge(Cartridge cartridge) {
        if (this.cartridge != null) {
            return false;
        }
        this.cartridge = cartridge;
        return true;
    }

    public boolean isInCartridge() {
        return cartridge != null;
    }

    public boolean isEmpty() {
        return cartridge == null;
    }

    public boolean canShoot() {
        return this.cartridge != null && this.cartridge.canShoot();
    }

    /** 排莢すべきか。空薬莢が入っている場合に true。 */
    public boolean shouldEject() {
        return this.cartridge != null && !this.cartridge.canShoot();
    }

    public Optional<BulletData> shoot() {
        if (this.cartridge == null) {
            return Optional.empty();
        }
        return this.cartridge.spend();
    }

    public Optional<Cartridge> ejectCartridge() {
        if (this.cartridge == null) {
            return Optional.empty();
        }
        var result = Optional.of(this.cartridge);
        this.cartridge = null;
        return result;
    }

    public void read(NbtCompound nbt) {
        this.cartridge = null;
        if (nbt.contains("cartridge")) {
            this.cartridge = new Cartridge(null);
            this.cartridge.read(nbt.getCompound("cartridge"));
        }
    }

    public void write(NbtCompound nbt) {
        if (this.cartridge != null) {
            var cartridgeNbt = new NbtCompound();
            this.cartridge.write(cartridgeNbt);
            nbt.put("cartridge", cartridgeNbt);
        }
    }
}

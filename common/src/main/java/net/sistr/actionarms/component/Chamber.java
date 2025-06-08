package net.sistr.actionarms.component;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Chamber {
    @Nullable
    private Cartridge cartridge;

    public Chamber(@Nullable Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    public Optional<Cartridge> getCartridge() {
        return Optional.ofNullable(cartridge);
    }

    public void setCartridge(@Nullable Cartridge cartridge) {
        this.cartridge = cartridge;
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

    public Optional<Bullet> shoot() {
        if (this.cartridge == null || this.cartridge.getBullet().isEmpty()) {
            return Optional.empty();
        }
        var bullet = this.cartridge.getBullet().get();
        this.cartridge.setBullet(null);
        return Optional.of(bullet);
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

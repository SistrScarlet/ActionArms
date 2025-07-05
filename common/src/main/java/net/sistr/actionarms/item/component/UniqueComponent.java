package net.sistr.actionarms.item.component;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;
import java.util.UUID;

// todo:コンポーネント化する意味が薄い
public class UniqueComponent implements IItemComponent {
    private UUID uniqueId;

    public static Optional<UUID> getUUID(ItemStack stack) {
        if (stack.getNbt() == null) return Optional.empty();
        var nbt = stack.getNbt();
        if (!nbt.containsUuid("uniqueId")) return Optional.empty();
        var uuid = nbt.getUuid("uniqueId");
        return Optional.of(uuid);
    }

    public static UUID getOrSet(ItemStack stack) {
        // queryだと値がセットされない
        var nbt = stack.getOrCreateNbt();
        if (!nbt.containsUuid("uniqueId")) {
            var uuid = UUID.randomUUID();
            nbt.putUuid("uniqueId", uuid);
            return uuid;
        }
        return nbt.getUuid("uniqueId");
    }

    @Override
    public void read(NbtCompound nbt) {
        if (nbt.containsUuid("uniqueId")) {
            this.uniqueId = nbt.getUuid("uniqueId");
        } else {
            this.uniqueId = UUID.randomUUID();
        }
    }

    @Override
    public void write(NbtCompound nbt) {
        nbt.putUuid("uniqueId", this.uniqueId);
    }

    public UUID getUUID() {
        return this.uniqueId;
    }

    public void setUUID(UUID uuid) {
        this.uniqueId = uuid;
    }

    public void reset() {
        this.uniqueId = UUID.randomUUID();
    }
}

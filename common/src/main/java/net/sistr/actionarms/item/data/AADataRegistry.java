package net.sistr.actionarms.item.data;

import java.util.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.sistr.actionarms.ActionArms;
import org.jetbrains.annotations.Nullable;

public class AADataRegistry {
    private static final Map<Class<? extends IData>, Map<String, ? extends IData>> registries =
            new HashMap<>();

    public static final BulletData MEDIUM_CALIBER_BULLET =
            new BulletData(
                    "medium_caliber_bullet",
                    ActionArms.getConfig().game.medium_caliber_bullet_damage,
                    ActionArms.getConfig().game.medium_caliber_bullet_headshot_damage);
    public static final MagazineData M1873_TUBE_MAGAZINE =
            new MagazineData("m1873_magazine", 10, bullet -> true);
    public static final LeverActionGunData M1873 =
            new LeverActionGunData(
                    "m1873", 0.3f, 0.2f, 0.2f, 0.1f, 0.2f, 0.05f, 0.05f, 0.05f, 1, 5.0f, 0.01f,
                    5.0f);
    public static final SAAGunData COLT_SAA =
            new SAAGunData(
                    "colt_saa",
                    /* cylinderCapacity= */ 6,
                    /* cockLength= */ 0.1f,
                    /* fireCoolLength= */ 0.05f,
                    /* ejectLength= */ 0.15f,
                    /* loadLength= */ 0.15f,
                    /* baseSpreadAngle= */ 5.0f,
                    /* aimSpreadAngle= */ 0.5f,
                    /* movementSpreadIncrease= */ 3.0f);

    @SuppressWarnings("unchecked")
    public static <T extends IData> Optional<T> getById(Class<T> type, String id) {
        Map<String, T> typeRegistry = (Map<String, T>) registries.get(type);
        if (typeRegistry != null) {
            return Optional.ofNullable(typeRegistry.get(id));
        }
        return Optional.empty();
    }

    private static void initializeRegistries() {
        registries.put(BulletData.class, new HashMap<>());
        registries.put(MagazineData.class, new HashMap<>());
        registries.put(LeverActionGunData.class, new HashMap<>());
        registries.put(SAAGunData.class, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static <T extends IData> void registerData(Class<T> type, T data) {
        Map<String, T> typeRegistry = (Map<String, T>) registries.get(type);
        if (typeRegistry != null) {
            typeRegistry.put(data.getId(), data);
        }
    }

    static {
        initializeRegistries();
        registerData(BulletData.class, MEDIUM_CALIBER_BULLET);
        registerData(MagazineData.class, M1873_TUBE_MAGAZINE);
        registerData(LeverActionGunData.class, M1873);
        registerData(SAAGunData.class, COLT_SAA);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IData> Set<String> getRegisteredIds(Class<T> type) {
        Map<String, T> typeRegistry = (Map<String, T>) registries.get(type);
        return typeRegistry != null ? typeRegistry.keySet() : Set.of();
    }

    @SuppressWarnings("unchecked")
    public static <T extends IData> Collection<T> getAllData(Class<T> type) {
        Map<String, T> typeRegistry = (Map<String, T>) registries.get(type);
        return typeRegistry != null ? typeRegistry.values() : Set.of();
    }

    public static <T extends IData> int getRegisteredCount(Class<T> type) {
        Map<String, ? extends IData> typeRegistry = registries.get(type);
        return typeRegistry != null ? typeRegistry.size() : 0;
    }

    public static <T extends IData> Optional<T> read(Class<T> type, NbtCompound nbt) {
        if (!nbt.contains("id")) {
            return Optional.empty();
        }
        var id = nbt.getString("id");
        return getById(type, id);
    }

    public static <T extends IData> Optional<T> read(Class<T> type, NbtCompound nbt, String id) {
        return read(type, nbt.getCompound(id));
    }

    public static <T extends IData> List<T> readAll(Class<T> type, NbtList nbt) {
        List<T> dataList = new ArrayList<>();
        for (NbtElement elem : nbt) {
            if (elem instanceof NbtCompound nbtCompound) {
                read(type, nbtCompound).ifPresent(dataList::add);
            }
        }
        return dataList;
    }

    public static <T extends IData> void write(T data, NbtCompound nbt) {
        nbt.putString("id", data.getId());
    }

    public static <T extends IData> void write(@Nullable T data, NbtCompound nbt, String id) {
        if (data == null) return;
        var dataNbt = new NbtCompound();
        write(data, dataNbt);
        nbt.put(id, dataNbt);
    }

    public static <T extends IData> void writeAll(Collection<T> dataList, NbtList list) {
        for (T data : dataList) {
            var dataNbt = new NbtCompound();
            write(data, dataNbt);
            list.add(dataNbt);
        }
    }
}

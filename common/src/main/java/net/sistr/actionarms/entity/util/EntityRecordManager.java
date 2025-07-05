package net.sistr.actionarms.entity.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EntityList;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EntityRecordManager {
    private static final EntityRecord[] ENTITY_RECORDS = new EntityRecord[0];
    private final Map<UUID, EntityRecord>[] recordMaps = new Map[10];
    private int index;

    public EntityRecordManager() {
        for (int i = 0; i < recordMaps.length; i++) {
            recordMaps[i] = new HashMap<>(100);
        }
    }

    public void preWorldTick(ServerWorld world, EntityList entityList) {
        index = (index + 1) % recordMaps.length;
        recordMaps[index].clear();
        entityList.forEach(e -> record(recordMaps[index], e));
    }

    public Optional<EntityRecord> getRecord(UUID id, int prev) {
        int targetIndex = (index - prev + recordMaps.length) % recordMaps.length;
        return Optional.ofNullable(recordMaps[targetIndex].get(id));
    }

    public void record(Map<UUID, EntityRecord> recordMap, Entity entity) {
        recordMap.put(entity.getUuid(),
                new EntityRecord(
                        entity.getUuid(),
                        entity.getPos(),
                        entity.getBoundingBox(),
                        entity.getEyePos()
                )
        );
        for (Entity passenger : entity.getPassengerList()) {
            record(recordMap, passenger);
        }
    }

    public record EntityRecord(UUID uuid, Vec3d pos, Box boundingBox, Vec3d eyePos) {

    }
}
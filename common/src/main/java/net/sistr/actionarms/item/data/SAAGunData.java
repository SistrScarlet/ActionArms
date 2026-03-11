package net.sistr.actionarms.item.data;

public record SAAGunData(
        String id,
        int cylinderCapacity,
        float cockLength,
        float fireCoolLength,
        float ejectLength,
        float loadLength,
        float baseSpreadAngle,
        float aimSpreadAngle,
        float movementSpreadIncrease)
        implements IData {
    @Override
    public String getId() {
        return this.id;
    }
}

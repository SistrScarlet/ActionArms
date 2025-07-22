package net.sistr.actionarms.item.data;

public record BulletData(String id, float damage, float headshotDamage) implements IData {
    @Override
    public String getId() {
        return this.id;
    }
}

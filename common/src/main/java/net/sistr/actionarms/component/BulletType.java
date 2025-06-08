package net.sistr.actionarms.component;

public record BulletType(String name, int damage) {
    public static final BulletType DEFAULT_TYPE = new BulletType("default", 2);
}

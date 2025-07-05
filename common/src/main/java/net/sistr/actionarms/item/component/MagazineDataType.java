package net.sistr.actionarms.item.component;

import java.util.function.Predicate;

public record MagazineDataType(int capacity, Predicate<BulletComponent> allowBullet) {
}

package net.sistr.actionarms.component;

import java.util.function.Predicate;

public record MagazineType(String name, int capacity, Predicate<Bullet> allowBullet) {
}

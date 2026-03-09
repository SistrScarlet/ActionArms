package net.sistr.actionarms.item.data;

import java.util.function.Predicate;

public record MagazineData(String id, int capacity, Predicate<BulletData> allowBullet)
    implements IData {
  @Override
  public String getId() {
    return this.id;
  }
}

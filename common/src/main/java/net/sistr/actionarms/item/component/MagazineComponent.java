package net.sistr.actionarms.item.component;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;
import net.sistr.actionarms.item.component.registry.GunDataTypes;

import java.util.*;

public class MagazineComponent implements IItemComponent {
    private final MagazineDataType magazineDataType;
    private final LinkedList<BulletDataType> bullets;

    public MagazineComponent(MagazineDataType magazineDataType) {
        this.magazineDataType = magazineDataType;
        this.bullets = new LinkedList<>();
    }

    public boolean addFirstBullet(BulletDataType bullet) {
        if (bullets.size() < magazineDataType.capacity()
                && this.magazineDataType.allowBullet().test(bullet)) {
            bullets.addFirst(bullet);
            return true;
        }
        return false;
    }

    public boolean addLastBullet(BulletDataType bullet) {
        if (bullets.size() < magazineDataType.capacity()
                && this.magazineDataType.allowBullet().test(bullet)) {
            bullets.addLast(bullet);
            return true;
        }
        return false;
    }

    public List<BulletDataType> addBullets(List<BulletDataType> bulletList, boolean reverse, boolean first) {
        var compat = new ArrayList<BulletDataType>();
        var incompat = new ArrayList<BulletDataType>();
        splitBullets(bulletList, compat, incompat);
        if (reverse) {
            compat = new ArrayList<>(compat);
            Collections.reverse(compat);
        }

        if (bullets.size() + compat.size() <= magazineDataType.capacity()) {
            if (first) {
                bullets.addAll(0, compat);
            } else {
                bullets.addAll(compat);
            }
        }
        return incompat;
    }

    public List<BulletDataType> addFirstBullets(List<BulletDataType> bulletList, boolean reverse) {
        return addBullets(bulletList, reverse, true);
    }

    public List<BulletDataType> addLastBullets(List<BulletDataType> bulletList, boolean reverse) {
        return addBullets(bulletList, reverse, false);
    }

    public void splitBullets(List<BulletDataType> bulletList, List<BulletDataType> compat, List<BulletDataType> incompat) {
        for (BulletDataType bullet : bulletList) {
            if (this.magazineDataType.allowBullet().test(bullet)) {
                compat.add(bullet);
            } else {
                incompat.add(bullet);
            }
        }
    }

    public void removeFirstBullet() {
        bullets.removeFirst();
    }

    public void removeLastBullet() {
        bullets.removeLast();
    }

    public Optional<BulletDataType> popFirstBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.removeFirst());
        }
        return Optional.empty();
    }

    public Optional<BulletDataType> popLastBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.removeLast());
        }
        return Optional.empty();
    }

    public Optional<BulletDataType> getFirstBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.getFirst());
        }
        return Optional.empty();
    }

    public Optional<BulletDataType> getLastBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.getLast());
        }
        return Optional.empty();
    }

    public boolean canAddBullet() {
        return bullets.size() < magazineDataType.capacity();
    }

    public boolean canAddBullet(BulletDataType bullet) {
        return this.magazineDataType.allowBullet().test(bullet);
    }

    public boolean hasBullet() {
        return !bullets.isEmpty();
    }

    public boolean isFull() {
        return bullets.size() >= magazineDataType.capacity();
    }

    public List<BulletDataType> getBullets() {
        return bullets;
    }

    public int getMaxCapacity() {
        return magazineDataType.capacity();
    }

    public boolean isEmpty() {
        return this.bullets.isEmpty();
    }

    public MagazineDataType getMagazineType() {
        return magazineDataType;
    }

    public int size() {
        return bullets.size();
    }

    public void clear() {
        bullets.clear();
    }

    public void read(NbtCompound nbt) {
        bullets.clear();
        var bulletList = nbt.getList("Bullets", 10);
        for (int i = 0; i < bulletList.size(); i++) {
            var bulletNbt = bulletList.getCompound(i);
            this.bullets.add(GunDataTypes.MEDIUM_CALIBER_BULLET);
        }
    }

    public void write(NbtCompound nbt) {
        var bulletList = new NbtList();
        for (BulletDataType bullet : bullets) {
            var bulletNbt = new NbtCompound();
            bulletList.add(bulletNbt);
        }
        nbt.put("Bullets", bulletList);
    }
}

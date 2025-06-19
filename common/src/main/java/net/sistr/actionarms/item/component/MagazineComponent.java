package net.sistr.actionarms.item.component;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;

import java.util.*;

public class MagazineComponent implements IItemComponent {
    private final MagazineDataType magazineDataType;
    private final LinkedList<BulletComponent> bullets;

    public MagazineComponent(MagazineDataType magazineDataType) {
        this.magazineDataType = magazineDataType;
        this.bullets = new LinkedList<>();
    }

    public boolean addFirstBullet(BulletComponent bullet) {
        if (bullets.size() < magazineDataType.capacity()
                && this.magazineDataType.allowBullet().test(bullet)) {
            bullets.addFirst(bullet);
            return true;
        }
        return false;
    }

    public boolean addLastBullet(BulletComponent bullet) {
        if (bullets.size() < magazineDataType.capacity()
                && this.magazineDataType.allowBullet().test(bullet)) {
            bullets.addLast(bullet);
            return true;
        }
        return false;
    }

    public List<BulletComponent> addBullets(List<BulletComponent> bulletList, boolean reverse, boolean first) {
        var compat = new ArrayList<BulletComponent>();
        var incompat = new ArrayList<BulletComponent>();
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

    public List<BulletComponent> addFirstBullets(List<BulletComponent> bulletList, boolean reverse) {
        return addBullets(bulletList, reverse, true);
    }

    public List<BulletComponent> addLastBullets(List<BulletComponent> bulletList, boolean reverse) {
        return addBullets(bulletList, reverse, false);
    }

    public void splitBullets(List<BulletComponent> bulletList, List<BulletComponent> compat, List<BulletComponent> incompat) {
        for (BulletComponent bullet : bulletList) {
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

    public Optional<BulletComponent> popFirstBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.removeFirst());
        }
        return Optional.empty();
    }

    public Optional<BulletComponent> popLastBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.removeLast());
        }
        return Optional.empty();
    }

    public Optional<BulletComponent> getFirstBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.getFirst());
        }
        return Optional.empty();
    }

    public Optional<BulletComponent> getLastBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.getLast());
        }
        return Optional.empty();
    }

    public boolean canAddBullet() {
        return bullets.size() < magazineDataType.capacity();
    }

    public boolean canAddBullet(BulletComponent bullet) {
        return this.magazineDataType.allowBullet().test(bullet);
    }

    public boolean hasBullet() {
        return !bullets.isEmpty();
    }

    public boolean isFull() {
        return bullets.size() >= magazineDataType.capacity();
    }

    public List<BulletComponent> getBullets() {
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
            var bullet = GunComponentTypes.MEDIUM_CALIBER_BULLET.get();
            bullet.read(bulletNbt);
            this.bullets.add(bullet);
        }
    }

    public void write(NbtCompound nbt) {
        var bulletList = new NbtList();
        for (BulletComponent bullet : bullets) {
            var bulletNbt = new NbtCompound();
            bullet.write(bulletNbt);
            bulletList.add(bulletNbt);
        }
        nbt.put("Bullets", bulletList);
    }
}

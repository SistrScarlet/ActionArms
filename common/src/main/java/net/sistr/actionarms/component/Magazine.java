package net.sistr.actionarms.component;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.*;

public class Magazine {
    private final MagazineType type;
    private final LinkedList<Bullet> bullets;

    public Magazine(MagazineType type) {
        this.type = type;
        this.bullets = new LinkedList<>();
    }

    public boolean addFirstBullet(Bullet bullet) {
        if (bullets.size() < type.capacity()
                && this.type.allowBullet().test(bullet)) {
            bullets.addFirst(bullet);
            return true;
        }
        return false;
    }

    public boolean addLastBullet(Bullet bullet) {
        if (bullets.size() < type.capacity()
                && this.type.allowBullet().test(bullet)) {
            bullets.addLast(bullet);
            return true;
        }
        return false;
    }

    public List<Bullet> addFirstBullets(List<Bullet> bulletList, boolean reverse, boolean first) {
        var compat = new ArrayList<Bullet>();
        var incompat = new ArrayList<Bullet>();
        splitBullets(bulletList, compat, incompat);
        if (reverse) {
            compat = new ArrayList<>(compat);
            Collections.reverse(compat);
        }

        if (bullets.size() + compat.size() <= type.capacity()) {
            if (first) {
                bullets.addAll(0, compat);
            } else {
                bullets.addAll(compat);
            }
        }
        return incompat;
    }

    public List<Bullet> addFirstBullets(List<Bullet> bulletList, boolean reverse) {
        return addFirstBullets(bulletList, reverse, true);
    }

    public List<Bullet> addLastBullets(List<Bullet> bulletList, boolean reverse) {
        return addFirstBullets(bulletList, reverse, false);
    }

    public void splitBullets(List<Bullet> bulletList, List<Bullet> compat, List<Bullet> incompat) {
        for (Bullet bullet : bulletList) {
            if (this.type.allowBullet().test(bullet)) {
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

    public Optional<Bullet> popFirstBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.removeFirst());
        }
        return Optional.empty();
    }

    public Optional<Bullet> popLastBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.removeLast());
        }
        return Optional.empty();
    }

    public Optional<Bullet> getFirstBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.getFirst());
        }
        return Optional.empty();
    }

    public Optional<Bullet> getLastBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.getLast());
        }
        return Optional.empty();
    }

    public boolean canAddBullet() {
        return bullets.size() < type.capacity();
    }

    public boolean canAddBullet(Bullet bullet) {
        return this.type.allowBullet().test(bullet);
    }

    public boolean hasBullet() {
        return !bullets.isEmpty();
    }

    public boolean isFull() {
        return bullets.size() >= type.capacity();
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public int getMaxCapacity() {
        return type.capacity();
    }

    public boolean isEmpty() {
        return this.bullets.isEmpty();
    }

    public MagazineType getType() {
        return type;
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
            var bullet = Bullet.read(bulletNbt);
            bullets.add(bullet);
        }
    }

    public void write(NbtCompound nbt) {
        var bulletList = new NbtList();
        for (Bullet bullet : bullets) {
            var bulletNbt = new NbtCompound();
            bullet.write(bulletNbt);
            bulletList.add(bulletNbt);
        }
        nbt.put("Bullets", bulletList);
    }
}

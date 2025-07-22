package net.sistr.actionarms.item.component;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.sistr.actionarms.item.data.AADataRegistry;
import net.sistr.actionarms.item.data.BulletData;
import net.sistr.actionarms.item.data.MagazineData;

import java.util.*;

public class MagazineComponent implements IComponent {
    private final MagazineData magazineData;
    private final LinkedList<BulletData> bullets;

    public MagazineComponent(MagazineData magazineData) {
        this.magazineData = magazineData;
        this.bullets = new LinkedList<>();
    }

    public boolean addFirstBullet(BulletData bullet) {
        if (bullets.size() < magazineData.capacity()
                && this.magazineData.allowBullet().test(bullet)) {
            bullets.addFirst(bullet);
            return true;
        }
        return false;
    }

    public boolean addLastBullet(BulletData bullet) {
        if (bullets.size() < magazineData.capacity()
                && this.magazineData.allowBullet().test(bullet)) {
            bullets.addLast(bullet);
            return true;
        }
        return false;
    }

    public List<BulletData> addBullets(List<BulletData> bulletList, boolean reverse, boolean first) {
        var compat = new ArrayList<BulletData>();
        var incompat = new ArrayList<BulletData>();
        splitBullets(bulletList, compat, incompat);
        if (reverse) {
            compat = new ArrayList<>(compat);
            Collections.reverse(compat);
        }

        if (bullets.size() + compat.size() <= magazineData.capacity()) {
            if (first) {
                bullets.addAll(0, compat);
            } else {
                bullets.addAll(compat);
            }
        }
        return incompat;
    }

    public List<BulletData> addFirstBullets(List<BulletData> bulletList, boolean reverse) {
        return addBullets(bulletList, reverse, true);
    }

    public List<BulletData> addLastBullets(List<BulletData> bulletList, boolean reverse) {
        return addBullets(bulletList, reverse, false);
    }

    public void splitBullets(List<BulletData> bulletList, List<BulletData> compat, List<BulletData> incompat) {
        for (BulletData bullet : bulletList) {
            if (this.magazineData.allowBullet().test(bullet)) {
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

    public Optional<BulletData> popFirstBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.removeFirst());
        }
        return Optional.empty();
    }

    public Optional<BulletData> popLastBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.removeLast());
        }
        return Optional.empty();
    }

    public Optional<BulletData> getFirstBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.getFirst());
        }
        return Optional.empty();
    }

    public Optional<BulletData> getLastBullet() {
        if (!bullets.isEmpty()) {
            return Optional.of(bullets.getLast());
        }
        return Optional.empty();
    }

    public boolean canAddBullet() {
        return bullets.size() < magazineData.capacity();
    }

    public boolean canAddBullet(BulletData bullet) {
        return this.magazineData.allowBullet().test(bullet);
    }

    public boolean hasBullet() {
        return !bullets.isEmpty();
    }

    public boolean isFull() {
        return bullets.size() >= magazineData.capacity();
    }

    public List<BulletData> getBullets() {
        return bullets;
    }

    public int getMaxCapacity() {
        return magazineData.capacity();
    }

    public boolean isEmpty() {
        return this.bullets.isEmpty();
    }

    public MagazineData getMagazineType() {
        return magazineData;
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
        this.bullets.addAll(AADataRegistry.readAll(BulletData.class, bulletList));
    }

    public void write(NbtCompound nbt) {
        var bulletList = new NbtList();
        AADataRegistry.writeAll(bullets, bulletList);
        nbt.put("Bullets", bulletList);
    }
}

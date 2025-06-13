package net.sistr.actionarms.hud;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.sistr.actionarms.item.component.LeverActionGunComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record LeverActionHudState(MagazineContents magazineContents,
                                  ChamberState chamberState) {

    public static LeverActionHudState of(LeverActionGunComponent component) {
        var list = component.getMagazine().getBullets().stream()
                .map(bulletComponent -> "middle_caliber")
                .toList();
        var chamberState = new ChamberState(false, null);
        if (component.getChamber().isInCartridge()) {
            if (component.getChamber().getCartridge().get().getBullet().isPresent()) {
                chamberState = new ChamberState(true, "middle_caliber");
            } else {
                chamberState = new ChamberState(true, null);
            }
        }
        return new LeverActionHudState(new MagazineContents(list), chamberState);
    }

    public static LeverActionHudState of(NbtCompound nbt) {
        MagazineContents magazineContents = new MagazineContents(List.of());
        if (nbt.contains("magazine")) {
            var magazine = nbt.getList("magazine", 8);
            List<String> bullets = magazine.stream()
                    .map(NbtString.class::cast)
                    .map(NbtString::asString)
                    .toList();
            magazineContents = new MagazineContents(bullets);
        }
        ChamberState chamberState = new ChamberState(false, null);
        if (nbt.contains("chamber")) {
            var chamberNbt = nbt.getCompound("chamber");
            boolean inCartridge = chamberNbt.getBoolean("inCartridge");
            if (chamberNbt.contains("bullet")) {
                var bullet = chamberNbt.getString("bullet");
                chamberState = new ChamberState(inCartridge, bullet);
            } else {
                chamberState = new ChamberState(inCartridge, null);
            }
        }
        return new LeverActionHudState(magazineContents, chamberState);
    }

    public NbtCompound write() {
        var nbt = new NbtCompound();
        if (this.magazineContents != null) {
            var magazineNbt = new NbtList();
            this.magazineContents.bullets().forEach(bullet -> magazineNbt.add(NbtString.of(bullet)));
            nbt.put("magazine", magazineNbt);
        }
        if (this.chamberState != null) {
            var chamberNbt = new NbtCompound();
            chamberNbt.putBoolean("inCartridge", this.chamberState.inCartridge);
            if (this.chamberState.bullet != null) {
                chamberNbt.putString("bullet", this.chamberState.bullet);
            }
            nbt.put("chamber", chamberNbt);
        }
        return nbt;
    }

    public record MagazineContents(List<String> bullets) {
    }

    public record ChamberState(boolean inCartridge, @Nullable String bullet) {
        public boolean canShoot() {
            return bullet != null;
        }
    }

}

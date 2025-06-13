package net.sistr.actionarms.client.render.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.hud.LeverActionHudState;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.UniqueComponent;

import java.util.Optional;

public class AAHudRenderer {
    public static final AAHudRenderer INSTANCE = new AAHudRenderer();
    private static final Identifier MIDDLE_CALIBER_BULLET
            = new Identifier("actionarms", "textures/item/bullet/middle_caliber_bullet.png");
    private static final Identifier MIDDLE_CALIBER_BULLET_FRAME
            = new Identifier("actionarms", "textures/item/bullet/middle_caliber_bullet_frame.png");

    public void render(DrawContext drawContext, float tickDelta) {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null) {
            return;
        }
        var textRenderer = client.textRenderer;
        var main = player.getMainHandStack();
        if (main.getItem() instanceof LeverActionGunItem leverAction) {
            var uuid = UniqueComponent.get(main);
            Optional<LeverActionHudState> optional
                    = ClientHudManager.INSTANCE.getState("lever_action@" + uuid, LeverActionHudState::of);
            optional.ifPresent(hudState ->
                    leverActionHud(drawContext, tickDelta, textRenderer, main, leverAction, hudState));
        }

    }

    private void leverActionHud(DrawContext drawContext, float tickDelta, TextRenderer textRenderer,
                                ItemStack stack,
                                LeverActionGunItem leverAction, LeverActionHudState hudState) {
        var defaultState = leverAction.getGunComponent().get();
        boolean loaded = hudState.chamberState().canShoot();

        int maxMagazineBullets = defaultState.getMagazine().getMaxCapacity();
        var bullets = hudState.magazineContents().bullets();

        int size = 16;
        int margin = 10; // 画面端からのマージン

        // ベース位置（右下）
        int baseX = drawContext.getScaledWindowWidth() - size - margin;
        int baseY = drawContext.getScaledWindowHeight() - (maxMagazineBullets + 1) * size - margin;

        // 薬室描画（上部）
        Identifier chamber;
        if (loaded) {
            chamber = MIDDLE_CALIBER_BULLET;
        } else {
            chamber = MIDDLE_CALIBER_BULLET_FRAME;
        }

        drawContext.drawTexture(chamber, baseX, baseY, 0, 0, size, size, size, size);
        drawContext.drawBorder(baseX - 1, baseY - 1, 18, 18, 0xFF000000);

        // マガジン描画（縦方向）
        int yOffset = size;
        for (int i = 0; i < maxMagazineBullets; i++) {
            Identifier texture;
            if (i < bullets.size()) {
                texture = MIDDLE_CALIBER_BULLET;
            } else {
                texture = MIDDLE_CALIBER_BULLET_FRAME;
            }
            drawContext.drawTexture(texture, baseX, baseY + yOffset, 0, 0, size, size, size, size);
            yOffset += size;
        }
    }

    /**
     * 横並び表示のHUD（画面中央上部に表示）
     * 将来的に設定で切り替え可能にする場合のために保持
     */
    private void leverActionHudHorizontal(DrawContext drawContext, float tickDelta, TextRenderer textRenderer,
                                          ItemStack stack,
                                          LeverActionGunItem leverAction, LeverActionHudState hudState) {
        var defaultState = leverAction.getGunComponent().get();
        boolean loaded = hudState.chamberState().canShoot();

        int maxMagazineBullets = defaultState.getMagazine().getMaxCapacity();
        var bullets = hudState.magazineContents().bullets();

        int size = 16;

        // ベース位置
        int baseX = (int) (drawContext.getScaledWindowWidth() * (1f / 2f)) - (maxMagazineBullets * size) / 2;
        int baseY = (int) (drawContext.getScaledWindowHeight() * (1f / 10f));

        // 薬室描画
        Identifier chamber;
        if (loaded) {
            chamber = MIDDLE_CALIBER_BULLET;
        } else {
            chamber = MIDDLE_CALIBER_BULLET_FRAME;
        }

        drawContext.drawTexture(chamber, baseX, baseY, 0, 0, size, size, size, size);
        drawContext.drawBorder(baseX - 1, baseY - 1, 18, 18, 0xFF000000);

        // マガジン描画
        int xOffset = size;
        for (int i = 0; i < maxMagazineBullets; i++) {
            Identifier texture;
            if (i < bullets.size()) {
                texture = MIDDLE_CALIBER_BULLET;
            } else {
                texture = MIDDLE_CALIBER_BULLET_FRAME;
            }
            drawContext.drawTexture(texture, baseX + xOffset, baseY, 0, 0, size, size, size, size);
            xOffset += size;
        }
    }

    public static class HudState {
        private final String id;
        private NbtCompound nbt;
        private long lastUpdateTime;

        public HudState(String id) {
            this.id = id;
        }

        public void setNbt(NbtCompound nbt) {
            this.nbt = nbt;
        }

        public void setLastUpdateTime(long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public String getId() {
            return id;
        }

        public NbtCompound getNbt() {
            return nbt;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }
    }

}

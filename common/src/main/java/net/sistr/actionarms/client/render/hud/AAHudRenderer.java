package net.sistr.actionarms.client.render.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.entity.util.IAimManager;
import net.sistr.actionarms.hud.BulletHitHudState;
import net.sistr.actionarms.hud.LeverActionHudState;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.LeverActionGunComponent;
import net.sistr.actionarms.item.component.UniqueComponent;
import net.sistr.actionarms.mixin.GameRendererInvoker;

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
            var uuid = UniqueComponent.getOrSet(main);
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

        // クロスヘア表示

        renderCrosshair(drawContext, leverAction, defaultState, tickDelta);

        // 弾数表示

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

    private void renderCrosshair(DrawContext drawContext, LeverActionGunItem gunItem,
                                 LeverActionGunComponent gunComponent,
                                 float tickDelta) {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null) return;

        // エイム時はモデルの照準を使うため、クロスヘア描画不要

        boolean isAim = HasAimManager.get(player)
                .map(IAimManager::isAiming)
                .orElse(false);
        if (isAim) {
            return;
        }

        // 基本パラメータ取得
        float currentSpread = gunItem.fireSpread(player, gunComponent);

        // 画面座標系への変換
        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();

        // FOVの取得と計算
        var gameRenderer = client.gameRenderer;
        float verticalFOV = (float) Math.toRadians(((GameRendererInvoker) gameRenderer)
                .invokeGetFov(gameRenderer.getCamera(), tickDelta, true));
        double pixelsPerRadian = screenHeight / (2.0 * Math.tan(verticalFOV / 2.0));

        // 拡散角をピクセル距離に変換
        double spreadRadius = Math.toRadians(currentSpread) * pixelsPerRadian;

        // クロスヘア描画
        renderCrosshairLines(client, drawContext, screenWidth / 2, screenHeight / 2, (int) spreadRadius);
    }

    private void renderCrosshairLines(MinecraftClient client, DrawContext drawContext, int centerX, int centerY, int spreadRadius) {
        // クロスヘアパラメータ
        int gap = 4;           // 中央の隙間
        int lineLength = 8;    // 線の長さ
        int thickness = 0;     // 線の太さ

        var bulletHitState = ClientHudManager.INSTANCE.getRawState("bullet_hit");

        // 色を設定
        int color = bulletHitState
                .filter(state -> state.getLastUpdateTime() < client.world.getTime())
                .map(state -> BulletHitHudState.of(state.getNbt()))
                .map(state -> state.kill() ? 0xFFFF0000 : 0xFF00FF00) // キル：赤, ヒット：緑
                .orElse(0xFFFFFFFF); // 何も無ければ白


        // 上下左右の線を描画
        // 上
        // クロスヘアは上だけ無し
        /*drawContext.fill(centerX - thickness - 1,
                centerY - gap - spreadRadius - lineLength,
                centerX + thickness + 1,
                centerY - gap - spreadRadius + 1, color);*/

        // 下
        drawContext.fill(centerX - thickness - 1,
                centerY + gap + spreadRadius,
                centerX + thickness + 1,
                centerY + gap + spreadRadius + lineLength + 1, color);

        // 左
        drawContext.fill(centerX - gap - spreadRadius - lineLength,
                centerY - thickness - 1,
                centerX - gap - spreadRadius + 1,
                centerY + thickness + 1, color);

        // 右
        drawContext.fill(centerX + gap + spreadRadius,
                centerY - thickness - 1,
                centerX + gap + spreadRadius + lineLength + 1,
                centerY + thickness + 1, color);
    }

}

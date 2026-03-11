package net.sistr.actionarms.client.render.hud;

import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.entity.util.IAimManager;
import net.sistr.actionarms.hud.BulletHitHudState;
import net.sistr.actionarms.hud.LeverActionHudState;
import net.sistr.actionarms.hud.SAAHudState;
import net.sistr.actionarms.item.ItemUniqueManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.SAAGunItem;
import net.sistr.actionarms.item.component.LeverActionGunComponent;
import net.sistr.actionarms.item.component.SAAGunComponent;
import net.sistr.actionarms.mixin.GameRendererInvoker;

public class AAHudRenderer {
    public static final AAHudRenderer INSTANCE = new AAHudRenderer();

    // SAA シリンダー回転アニメーション用
    private int prevFiringIndex = -1;
    private float prevRotation;
    private float currentRotation;
    private float targetRotation;
    private static final Identifier MEDIUM_CALIBER_BULLET =
            new Identifier("actionarms", "textures/item/bullet/medium_caliber_bullet.png");
    private static final Identifier MEDIUM_CALIBER_BULLET_FRAME =
            new Identifier("actionarms", "textures/item/bullet/medium_caliber_bullet_frame.png");
    private static final Identifier MEDIUM_CALIBER_CARTRIDGE =
            new Identifier("actionarms", "textures/item/bullet/medium_caliber_cartridge.png");

    /** 毎 tick 呼び出し。回転アニメーションの更新。 */
    public void tick() {
        prevRotation = currentRotation;
        // 1tick で target に到達
        currentRotation = targetRotation;
    }

    public void render(DrawContext drawContext, float tickDelta) {
        var client = MinecraftClient.getInstance();
        if (client.gameRenderer.getCamera().isThirdPerson()) {
            return;
        }
        var player = client.player;
        if (player == null) {
            return;
        }
        var textRenderer = client.textRenderer;
        var main = player.getMainHandStack();
        if (main.getItem() instanceof LeverActionGunItem leverAction) {
            var uuid = ItemUniqueManager.INSTANCE.getOrSet(main);
            Optional<LeverActionHudState> optional =
                    ClientHudManager.INSTANCE.getState(
                            "lever_action@" + uuid, LeverActionHudState::of);
            optional.ifPresent(
                    hudState ->
                            leverActionHud(
                                    drawContext,
                                    tickDelta,
                                    textRenderer,
                                    main,
                                    leverAction,
                                    hudState));
        }

        if (main.getItem() instanceof SAAGunItem saaGunItem) {
            var uuid = ItemUniqueManager.INSTANCE.getOrSet(main);
            Optional<SAAHudState> optional =
                    ClientHudManager.INSTANCE.getState("saa@" + uuid, SAAHudState::of);
            optional.ifPresent(
                    hudState ->
                            saaHud(
                                    drawContext,
                                    tickDelta,
                                    textRenderer,
                                    main,
                                    saaGunItem,
                                    hudState));
        }
    }

    private void leverActionHud(
            DrawContext drawContext,
            float tickDelta,
            TextRenderer textRenderer,
            ItemStack stack,
            LeverActionGunItem leverAction,
            LeverActionHudState hudState) {
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
            chamber = MEDIUM_CALIBER_BULLET;
        } else {
            chamber = MEDIUM_CALIBER_BULLET_FRAME;
        }

        drawContext.drawTexture(chamber, baseX, baseY, 0, 0, size, size, size, size);
        drawContext.drawBorder(baseX - 1, baseY - 1, 18, 18, 0xFF000000);

        // マガジン描画（縦方向）
        int yOffset = size;
        for (int i = 0; i < maxMagazineBullets; i++) {
            Identifier texture;
            if (i < bullets.size()) {
                texture = MEDIUM_CALIBER_BULLET;
            } else {
                texture = MEDIUM_CALIBER_BULLET_FRAME;
            }
            drawContext.drawTexture(texture, baseX, baseY + yOffset, 0, 0, size, size, size, size);
            yOffset += size;
        }
    }

    private void saaHud(
            DrawContext drawContext,
            float tickDelta,
            TextRenderer textRenderer,
            ItemStack stack,
            SAAGunItem saaGunItem,
            SAAHudState hudState) {
        var defaultComponent = saaGunItem.getGunComponent().get();

        // クロスヘア表示
        renderSAACrosshair(drawContext, saaGunItem, defaultComponent, tickDelta);

        // シリンダー HUD 描画（円形配置、firingIndex に基づいて回転）
        int size = 16;
        int margin = 10;
        int radius = 30;
        var chamberStates = hudState.chamberStates();
        int chamberCount = chamberStates.size();

        // 円の中心位置（右下）
        int centerX = drawContext.getScaledWindowWidth() - margin - radius - size / 2;
        int centerY = drawContext.getScaledWindowHeight() - margin - radius - size / 2;

        // 回転アニメーション: firingIndex 変化時に target を更新
        int currentIndex = hudState.firingIndex();
        float stepAngle = (float) (2.0 * Math.PI / chamberCount);

        if (prevFiringIndex == -1) {
            prevFiringIndex = currentIndex;
            targetRotation = -stepAngle * currentIndex;
            prevRotation = targetRotation;
            currentRotation = targetRotation;
        } else if (currentIndex != prevFiringIndex) {
            int diff = currentIndex - prevFiringIndex;
            if (diff > chamberCount / 2) diff -= chamberCount;
            if (diff < -chamberCount / 2) diff += chamberCount;
            targetRotation += -stepAngle * diff;
            prevFiringIndex = currentIndex;
        }

        // tick 間を tickDelta で線形補間
        double rotationOffset = prevRotation + (currentRotation - prevRotation) * tickDelta;

        for (int i = 0; i < chamberCount; i++) {
            // 各薬室の角度: 上から時計回り、firingIndex 分だけ回転
            double angle = 2.0 * Math.PI * i / chamberCount - Math.PI / 2.0 + rotationOffset;
            int x = centerX + (int) (radius * Math.cos(angle)) - size / 2;
            int y = centerY + (int) (radius * Math.sin(angle)) - size / 2;

            SAAHudState.ChamberState state = chamberStates.get(i);
            switch (state) {
                case LOADED:
                    drawContext.drawTexture(
                            MEDIUM_CALIBER_BULLET, x, y, 0, 0, size, size, size, size);
                    break;
                case SPENT:
                    drawContext.drawTexture(
                            MEDIUM_CALIBER_CARTRIDGE, x, y, 0, 0, size, size, size, size);
                    break;
                case EMPTY:
                default:
                    drawContext.drawTexture(
                            MEDIUM_CALIBER_BULLET_FRAME, x, y, 0, 0, size, size, size, size);
                    break;
            }

            // 射撃位置マーカー（デフォ赤、撃てる状態で黒）
            if (i == hudState.firingIndex()) {
                int markerColor = hudState.hammerCocked() ? 0xFF000000 : 0xFFFF0000;
                drawContext.drawBorder(x - 1, y - 1, size + 2, size + 2, markerColor);
            }

            // ゲート開放中: 操作対象（ゲート位置）の薬室を白枠で囲む
            int gateIndex = (hudState.firingIndex() + 1) % chamberCount;
            if (hudState.gateOpen() && i == gateIndex) {
                drawContext.drawBorder(x - 1, y - 1, size + 2, size + 2, 0xFFFFFFFF);
            }
        }
    }

    private void renderSAACrosshair(
            DrawContext drawContext,
            SAAGunItem gunItem,
            SAAGunComponent gunComponent,
            float tickDelta) {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null) return;

        boolean isAim = HasAimManager.get(player).map(IAimManager::isAiming).orElse(false);
        if (isAim) {
            return;
        }

        float currentSpread = gunItem.fireSpread(player, gunComponent);

        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();

        var gameRenderer = client.gameRenderer;
        float verticalFOV =
                (float)
                        Math.toRadians(
                                ((GameRendererInvoker) gameRenderer)
                                        .invokeGetFov(gameRenderer.getCamera(), tickDelta, true));
        double pixelsPerRadian = screenHeight / (2.0 * Math.tan(verticalFOV / 2.0));
        double spreadRadius = Math.toRadians(currentSpread) * pixelsPerRadian;

        renderCrosshairLines(
                client, drawContext, screenWidth / 2, screenHeight / 2, (int) spreadRadius);
    }

    private void renderCrosshair(
            DrawContext drawContext,
            LeverActionGunItem gunItem,
            LeverActionGunComponent gunComponent,
            float tickDelta) {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null) return;

        // エイム時はモデルの照準を使うため、クロスヘア描画不要

        boolean isAim = HasAimManager.get(player).map(IAimManager::isAiming).orElse(false);
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
        float verticalFOV =
                (float)
                        Math.toRadians(
                                ((GameRendererInvoker) gameRenderer)
                                        .invokeGetFov(gameRenderer.getCamera(), tickDelta, true));
        double pixelsPerRadian = screenHeight / (2.0 * Math.tan(verticalFOV / 2.0));

        // 拡散角をピクセル距離に変換
        double spreadRadius = Math.toRadians(currentSpread) * pixelsPerRadian;

        // クロスヘア描画
        renderCrosshairLines(
                client, drawContext, screenWidth / 2, screenHeight / 2, (int) spreadRadius);
    }

    private void renderCrosshairLines(
            MinecraftClient client,
            DrawContext drawContext,
            int centerX,
            int centerY,
            int spreadRadius) {
        // クロスヘアパラメータ
        int gap = 4; // 中央の隙間
        int lineLength = 8; // 線の長さ
        int thickness = 0; // 線の太さ

        var bulletHitState = ClientHudManager.INSTANCE.getRawState("bullet_hit");

        // 色を設定
        int color =
                bulletHitState
                        .filter(state -> state.getLastUpdateTime() < client.world.getTime())
                        .map(state -> BulletHitHudState.of(state.getNbt()))
                        .map(state -> state.state().color())
                        .orElse(0xFFFFFFFF); // 何も無ければ白

        // 上下左右の線を描画
        // 上
        // クロスヘアは上だけ無し
        /*drawContext.fill(centerX - thickness - 1,
        centerY - gap - spreadRadius - lineLength,
        centerX + thickness + 1,
        centerY - gap - spreadRadius + 1, color);*/

        // 下
        drawContext.fill(
                centerX - thickness - 1,
                centerY + gap + spreadRadius,
                centerX + thickness + 1,
                centerY + gap + spreadRadius + lineLength + 1,
                color);

        // 左
        drawContext.fill(
                centerX - gap - spreadRadius - lineLength,
                centerY - thickness - 1,
                centerX - gap - spreadRadius + 1,
                centerY + thickness + 1,
                color);

        // 右
        drawContext.fill(
                centerX + gap + spreadRadius,
                centerY - thickness - 1,
                centerX + gap + spreadRadius + lineLength + 1,
                centerY + thickness + 1,
                color);
    }
}

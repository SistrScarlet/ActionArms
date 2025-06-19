package net.sistr.actionarms.client.key;

import net.minecraft.client.MinecraftClient;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.entity.util.KeyInputManager;
import net.sistr.actionarms.network.AimPacket;

public class ClientAimManager {
    public static final ClientAimManager INSTANCE = new ClientAimManager();
    private long lastUpdateTime = -1;
    private float pitchInc;
    private float yawInc;
    private float totalPitch;
    private float totalYaw;

    public void preTick() {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null) {
            return;
        }
        var aimManager = ((HasAimManager) player).actionArms$getAimManager();
        var keyManager = ClientKeyInputManager.INSTANCE.getKeyInputManager();
        boolean prevAiming = aimManager.isAiming();
        if (ActionArms.getConfig().key.aimToggle) {
            // トグルエイム動作
            // キーがターンオンするたびに、エイム状態がトグルで切り替わる
            if (keyManager.isTurnPress(KeyInputManager.Key.AIM)) {
                aimManager.setAiming(!prevAiming);
            }
        } else {
            // プッシュエイム動作
            // キーがターンオンでエイム状態がオン、キーがターンオフでエイム状態がオフになる
            if (keyManager.isTurnPress(KeyInputManager.Key.AIM)) {
                aimManager.setAiming(true);
            } else if (keyManager.isTurnRelease(KeyInputManager.Key.AIM)) {
                aimManager.setAiming(false);
            }
        }

        // リロード操作しているか、インベントリを開いている場合はキャンセル
        if (keyManager.isTurnPress(KeyInputManager.Key.RELOAD)
                || client.currentScreen != null) {
            aimManager.setAiming(false);
        }

        if (prevAiming != aimManager.isAiming()) {
            AimPacket.sendC2S(aimManager.isAiming());
        }

        // エイム中は少し視点をブレさせる
        if (aimManager.isAiming()) {
            var world = client.world;
            if (world == null) {
                lastUpdateTime = -1;
                totalPitch = 0;
                totalYaw = 0;
                return;
            }
            long nowTime = world.getTime();
            // 一定時間ごとに更新
            if (lastUpdateTime == -1 || lastUpdateTime + 40 + 20 * player.getRandom().nextFloat() < nowTime) {
                pitchInc = 1;
                yawInc = 1;
                var rand = player.getRandom();
                // 円範囲
                while (pitchInc * pitchInc + yawInc * yawInc > 1) {
                    // 方向の偏りを減らすため、合算が0に近づく方へ移動させる
                    pitchInc = rand.nextFloat() * (totalPitch > 0 ? -1 : 1);
                    yawInc = rand.nextFloat() * (totalYaw > 0 ? -1 : 1);
                }
                float length = (float) Math.sqrt(pitchInc * pitchInc + yawInc * yawInc);
                pitchInc /= length;
                yawInc /= length;
                pitchInc *= (0.8f + 0.2f * rand.nextFloat());
                yawInc *= (0.8f + 0.2f * rand.nextFloat());
                lastUpdateTime = nowTime;
            }
            player.setPitch(player.getPitch() + pitchInc * 0.1f);
            player.setYaw(player.getYaw() + yawInc * 0.1f);
            totalPitch += pitchInc * 0.2f;
            totalYaw += yawInc * 0.2f;
        } else {
            lastUpdateTime = -1;
            totalPitch = 0;
            totalYaw = 0;
        }
    }

}

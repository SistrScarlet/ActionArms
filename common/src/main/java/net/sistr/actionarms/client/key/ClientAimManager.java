package net.sistr.actionarms.client.key;

import net.minecraft.client.MinecraftClient;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.entity.util.KeyInputManager;
import net.sistr.actionarms.network.AimPacket;

public class ClientAimManager {
    public static final ClientAimManager INSTANCE = new ClientAimManager();

    public void preTick() {
        var player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        var aimManager = ((HasAimManager) player).actionArms$getAimManager();
        var keyManager = ClientKeyInputManager.INSTANCE.getKeyInputManager();
        if (ActionArms.getConfig().key.aimToggle) {
            // トグルエイム動作
            // キーがターンオンするたびに、エイム状態がトグルで切り替わる
            if (keyManager.isTurnPress(KeyInputManager.Key.AIM)) {
                boolean prevAiming = aimManager.isAiming();
                aimManager.setAiming(!prevAiming);
                AimPacket.sendC2S(aimManager.isAiming());
            }
        } else {
            // プッシュエイム動作
            // キーがターンオンでエイム状態がオン、キーがターンオフでエイム状態がオフになる
            if (keyManager.isTurnPress(KeyInputManager.Key.AIM)) {
                aimManager.setAiming(true);
                AimPacket.sendC2S(aimManager.isAiming());
            } else if (keyManager.isTurnRelease(KeyInputManager.Key.AIM)) {
                aimManager.setAiming(false);
                AimPacket.sendC2S(aimManager.isAiming());
            }
        }
    }

}

package net.sistr.actionarms.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.entity.util.HasKeyInputManager;
import net.sistr.actionarms.entity.util.KeyInputManager;

import java.util.HashMap;
import java.util.Map;

public class KeyInputPacket {
    public static final Identifier ID = new Identifier(ActionArms.MOD_ID, "key_input");

    public static void sendC2S(Map<KeyInputManager.Key, Boolean> keyStates) {
        var buf = createC2SPacket(keyStates);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Map<KeyInputManager.Key, Boolean> keyStates) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        // キーの数を書き込む
        buf.writeVarInt(keyStates.size());
        // 各キーの状態を書き込む
        keyStates.forEach((key, pressed) -> {
            buf.writeVarInt(key.ordinal());
            buf.writeBoolean(pressed);
        });
        return buf;
    }

    public static void receiveC2S(PacketByteBuf buf, NetworkManager.PacketContext context) {
        // 受信したキー入力の数を読み取る
        int count = buf.readVarInt();
        Map<KeyInputManager.Key, Boolean> keyStates = new HashMap<>();

        // 各キーの状態を読み取る
        for (int i = 0; i < count; i++) {
            int keyOrdinal = buf.readVarInt();
            boolean pressed = buf.readBoolean();
            KeyInputManager.Key key = KeyInputManager.Key.values()[keyOrdinal];
            keyStates.put(key, pressed);
        }

        var player = context.getPlayer();

        // メインスレッドで処理する
        context.queue(() -> {
            // 各キーの状態を更新
            keyStates.forEach((key, pressed) -> {
                var keyInputManager = ((HasKeyInputManager) player).actionArms$getKeyInputManager();
                keyInputManager.input(key, pressed);
            });
        });
    }
}

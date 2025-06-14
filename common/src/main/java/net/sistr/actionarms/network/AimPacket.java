package net.sistr.actionarms.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.entity.util.HasAimManager;

public class AimPacket {
    public static final Identifier ID = new Identifier(ActionArms.MOD_ID, "aim");

    public static void sendC2S(boolean aim) {
        var buf = createC2SPacket(aim);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(boolean aim) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBoolean(aim);
        return buf;
    }

    public static void receiveC2S(PacketByteBuf buf, NetworkManager.PacketContext context) {
        boolean aim = buf.readBoolean();
        var player = context.getPlayer();
        context.queue(() -> {
            ((HasAimManager) player).actionArms$getAimManager().setAiming(aim);
        });
    }
}

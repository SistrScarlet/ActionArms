package net.sistr.actionarms.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;

public class RecoilPacket {
    public static final Identifier ID = new Identifier(ActionArms.MOD_ID, "recoil");

    public static void sendS2C(ServerPlayerEntity sendPlayer) {
        var buf = createS2CPacket();
        NetworkManager.sendToPlayer(sendPlayer, ID, buf);
    }

    public static PacketByteBuf createS2CPacket() {
        var buf = new PacketByteBuf(Unpooled.buffer());
        return buf;
    }

    public static void receiveS2C(PacketByteBuf buf, NetworkManager.PacketContext context) {
        var player = context.getPlayer();
        context.queue(() -> {
            player.setPitch(player.getPitch() - 5);
        });
    }
}

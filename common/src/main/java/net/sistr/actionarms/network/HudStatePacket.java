package net.sistr.actionarms.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.hud.ClientHudManager;

public class HudStatePacket {
    public static final Identifier ID = new Identifier(ActionArms.MOD_ID, "hud_state");

    public static void sendS2C(ServerPlayerEntity sendPlayer, String stateId, NbtCompound nbt) {
        var buf = createS2CPacket(stateId, nbt);
        NetworkManager.sendToPlayer(sendPlayer, ID, buf);
    }

    public static PacketByteBuf createS2CPacket(String stateId, NbtCompound nbt) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(stateId);
        buf.writeNbt(nbt);
        return buf;
    }

    public static void receiveS2C(PacketByteBuf buf, NetworkManager.PacketContext context) {
        String stateId = buf.readString();
        var nbt = buf.readNbt();
        context.queue(() -> {
            ClientHudManager.INSTANCE.updateHud(stateId, nbt);
        });
    }

}

package net.sistr.actionarms.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.manager.ItemAnimationManager;

import java.util.UUID;

public class ItemAnimationEventPacket {
    public static final Identifier ID = new Identifier(ActionArms.MOD_ID, "item_animation_event");

    public static void sendS2C(World world, UUID uuid, String animationId, float seconds) {
        var buf = createS2CPacket(uuid, animationId, seconds);
        var players = world.getPlayers()
                .stream()
                .map(p -> (ServerPlayerEntity) p)
                .toList();
        NetworkManager.sendToPlayers(players, ID, buf);
    }

    public static PacketByteBuf createS2CPacket(UUID uuid, String animationId, float seconds) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(uuid);
        buf.writeString(animationId);
        buf.writeFloat(seconds);
        return buf;
    }

    public static void receiveS2C(PacketByteBuf buf, NetworkManager.PacketContext context) {
        UUID uuid = buf.readUuid();
        String animationId = buf.readString();
        float seconds = buf.readFloat();
        context.queue(() -> {
            ItemAnimationManager.INSTANCE.setAnimation(uuid, animationId, seconds);
        });
    }

}

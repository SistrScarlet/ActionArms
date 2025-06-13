package net.sistr.actionarms.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

public class Networking {

    public static void init() {
        if (Platform.getEnv() == EnvType.CLIENT) {
            initClient();
        }

        registerServerReceiver(KeyInputPacket.ID, KeyInputPacket::receiveC2S);
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        registerClientReceiver(ItemAnimationEventPacket.ID, ItemAnimationEventPacket::receiveS2C);
        registerClientReceiver(HudStatePacket.ID, HudStatePacket::receiveS2C);
    }

    private static void registerServerReceiver(Identifier id, NetworkManager.NetworkReceiver receiver) {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, id, receiver);
    }

    private static void registerClientReceiver(Identifier id, NetworkManager.NetworkReceiver receiver) {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, id, receiver);
    }

}

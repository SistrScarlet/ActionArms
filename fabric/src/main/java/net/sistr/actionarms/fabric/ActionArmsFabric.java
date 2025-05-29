package net.sistr.actionarms.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.ActionArmsClient;
import net.sistr.actionarms.client.key.AAKeys;
import net.sistr.actionarms.config.AAConfig;
import net.sistr.actionarms.network.Networking;
import net.sistr.actionarms.setup.Registration;

public class ActionArmsFabric implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        ActionArms.init();
        Registration.init();
        Networking.init();
    }

    @Override
    public void onInitializeClient() {
        ActionArmsClient.init();
        AAKeys.init();
    }
}

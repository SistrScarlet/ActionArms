package net.sistr.actionarms.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.ActionArmsClient;

public class ActionArmsFabric implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        ActionArms.preInit();
        ActionArms.init();
    }

    @Override
    public void onInitializeClient() {
        ActionArmsClient.preInit();
        ActionArmsClient.init();
    }
}

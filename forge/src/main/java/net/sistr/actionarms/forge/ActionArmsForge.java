package net.sistr.actionarms.forge;

import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.ActionArmsClient;
import net.sistr.actionarms.config.AAConfig;

@Mod(ActionArms.MOD_ID)
public class ActionArmsForge {
    public ActionArmsForge() {
        @SuppressWarnings("removal") var context = FMLJavaModLoadingContext.get();
        var eventBus = context.getModEventBus();

        EventBuses.registerModEventBus(ActionArms.MOD_ID, eventBus);

        eventBus.addListener(this::modInit);
        eventBus.addListener(this::clientInit);

        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> AutoConfig.getConfigScreen(AAConfig.class, parent).get()));

        ActionArms.preInit();

        // clientInitだと実行が遅い場合のクライアント処理
        if (Platform.getEnv() == Dist.CLIENT) {
            ActionArmsClient.preInit();
        }
    }

    public void modInit(FMLCommonSetupEvent event) {
        ActionArms.init();
    }

    public void clientInit(FMLClientSetupEvent event) {
        ActionArmsClient.init();
    }
}

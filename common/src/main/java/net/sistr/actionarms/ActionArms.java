package net.sistr.actionarms;

import com.mojang.logging.LogUtils;
import dev.architectury.event.events.common.TickEvent;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.sistr.actionarms.config.AAConfig;
import net.sistr.actionarms.entity.util.HasKeyInputManager;
import net.sistr.actionarms.hud.ServerHudManager;
import net.sistr.actionarms.item.ItemUniqueManager;
import net.sistr.actionarms.network.Networking;
import net.sistr.actionarms.setup.Registration;
import org.slf4j.Logger;

public class ActionArms {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "actionarms";
    private static ConfigHolder<AAConfig> CONFIG_HOLDER;

    public static void init() {
        Networking.init();
        TickEvent.SERVER_LEVEL_POST.register(world -> {
            if (world.isClient) return;
            ItemUniqueManager.INSTANCE.clearOld(world);
            world.getPlayers().forEach(player -> {
                var keyInputManager = ((HasKeyInputManager) player).actionArms$getKeyInputManager();
                keyInputManager.tick(); // キーパケットの受け取りがtick後のため、このタイミングでないとprevとnowが同じになる
            });
            ServerHudManager.INSTANCE.tick(world);
        });
    }

    public static void preInit() {
        Registration.init();

        AutoConfig.register(AAConfig.class, Toml4jConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(AAConfig.class);
    }

    public static AAConfig getConfig() {
        return CONFIG_HOLDER.getConfig();
    }

}

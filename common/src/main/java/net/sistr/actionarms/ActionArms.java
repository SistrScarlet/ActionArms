package net.sistr.actionarms;

import com.mojang.logging.LogUtils;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.sistr.actionarms.config.AAConfig;
import net.sistr.actionarms.network.Networking;
import net.sistr.actionarms.setup.Registration;
import org.slf4j.Logger;

public class ActionArms {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "actionarms";
    private static ConfigHolder<AAConfig> CONFIG_HOLDER;

    public static void init() {
        Registration.init();
        Networking.init();
    }

    public static void preInit() {
        AutoConfig.register(AAConfig.class, Toml4jConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(AAConfig.class);
    }

    public static AAConfig getConfig() {
        return CONFIG_HOLDER.getConfig();
    }

}

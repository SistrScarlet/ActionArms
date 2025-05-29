package net.sistr.actionarms;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.sistr.actionarms.config.AAConfig;

public class ActionArms {
    public static final String MOD_ID = "actionarms";
    private static ConfigHolder<AAConfig> CONFIG_HOLDER;

    public static void init() {
        AutoConfig.register(AAConfig.class, Toml4jConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(AAConfig.class);
    }

    public static AAConfig getConfig() {
        return CONFIG_HOLDER.getConfig();
    }

}

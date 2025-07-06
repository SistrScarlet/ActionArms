package net.sistr.actionarms.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.sistr.actionarms.ActionArms;

@Config(name = ActionArms.MOD_ID)
public class AAConfig implements ConfigData {
    @ConfigEntry.Category("key")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Key key = new Key();

    public static class Key {
        @ConfigEntry.Gui.Tooltip
        public boolean aimToggle = false;
    }

    @ConfigEntry.Category("key")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Game game = new Game();

    public static class Game {
        @ConfigEntry.Gui.RequiresRestart
        public int medium_caliber_bullet_damage = 9;
        @ConfigEntry.Gui.RequiresRestart
        public int medium_caliber_bullet_headshot_damage = 12;
    }
}

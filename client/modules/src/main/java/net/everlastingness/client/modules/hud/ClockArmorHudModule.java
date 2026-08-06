package net.everlastingness.client.modules.hud;

import net.everlastingness.client.common.module.AbstractModule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Clock / Armor Durability HUD module — displays the current real-world time
 * and armor durability bars in the HUD, matching Lunar/Badlion's
 * "Potion HUD" + "Clock" overlay features.
 *
 * <p>On MC 1.7.10, the per-version mixin calls getTimeString() and
 * getArmorInfo() from the render tick, and the HUD renderer displays them
 * below the coordinates/FPS lines.</p>
 */
public class ClockArmorHudModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/ClockHud");
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss");

    @Override
    public String getId() { return "clock_armor_hud"; }

    @Override
    public String getName() { return "Clock & Armor HUD"; }

    @Override
    public String getDescription() {
        return "Displays real-world time and armor durability in the HUD.";
    }

    @Override
    public String getCategory() { return "HUD"; }

    /** Current time string for HUD display. */
    public String getTimeString() {
        return TIME_FMT.format(new Date());
    }

    @Override
    public void onEnable() { LOGGER.info("Clock & Armor HUD enabled"); }
    @Override
    public void onDisable() { LOGGER.info("Clock & Armor HUD disabled"); }
}

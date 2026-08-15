package net.everlastingness.client.modules.combat;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Toggle Sneak/Sprint module — makes sneak and sprint sticky (press to toggle)
 * instead of hold, mirroring Lunar's ToggleSneak mod.
 */
public class ToggleSneakModule extends AbstractModule {
    @Override public String getId() { return "toggle_sneak"; }
    @Override public String getName() { return "Toggle Sneak"; }
    @Override public String getDescription() { return "Toggle sneak/sprint instead of holding."; }
    @Override public String getCategory() { return "COMBAT"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

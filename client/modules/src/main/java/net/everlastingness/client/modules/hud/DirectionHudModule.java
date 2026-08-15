package net.everlastingness.client.modules.hud;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Direction HUD module — shows the facing direction (N/NE/E/.../NW) and
 * yaw angle, mirroring Lunar's DirectionHud mod.
 */
public class DirectionHudModule extends AbstractModule {
    @Override public String getId() { return "direction_hud"; }
    @Override public String getName() { return "Direction HUD"; }
    @Override public String getDescription() { return "Shows your facing direction and yaw."; }
    @Override public String getCategory() { return "HUD"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

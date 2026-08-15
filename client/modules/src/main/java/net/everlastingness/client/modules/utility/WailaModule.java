package net.everlastingness.client.modules.utility;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Waila module — "What Am I Looking At": shows the name of the targeted
 * block or entity near the crosshair, mirroring Lunar's Waila mod.
 */
public class WailaModule extends AbstractModule {
    @Override public String getId() { return "waila"; }
    @Override public String getName() { return "Waila"; }
    @Override public String getDescription() { return "Shows what you are looking at."; }
    @Override public String getCategory() { return "UTILITY"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

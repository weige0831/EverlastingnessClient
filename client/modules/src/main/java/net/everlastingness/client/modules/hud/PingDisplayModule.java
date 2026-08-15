package net.everlastingness.client.modules.hud;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Ping Display module — shows the latency to the current server in the HUD,
 * mirroring Lunar's Ping mod. On 1.7.10-1.8 the value comes from the tab-list
 * response time (the player's ping field); in singleplayer it reads 0.
 */
public class PingDisplayModule extends AbstractModule {
    @Override public String getId() { return "ping_display"; }
    @Override public String getName() { return "Ping Display"; }
    @Override public String getDescription() { return "Shows your latency to the server in ms."; }
    @Override public String getCategory() { return "HUD"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

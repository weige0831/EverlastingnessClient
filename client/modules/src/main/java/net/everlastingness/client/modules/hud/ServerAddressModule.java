package net.everlastingness.client.modules.hud;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Server Address module — shows the IP of the current server, mirroring
 * Lunar's ServerAddress mod. Blank in singleplayer.
 */
public class ServerAddressModule extends AbstractModule {
    @Override public String getId() { return "server_address"; }
    @Override public String getName() { return "Server Address"; }
    @Override public String getDescription() { return "Shows the current server IP."; }
    @Override public String getCategory() { return "HUD"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

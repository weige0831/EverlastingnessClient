package net.everlastingness.client.modules.hud;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Memory Usage module — shows used/allocated/max JVM RAM, mirroring Lunar's
 * Memory mod. Reads from Runtime.getRuntime().
 */
public class MemoryUsageModule extends AbstractModule {
    @Override public String getId() { return "memory_usage"; }
    @Override public String getName() { return "Memory Usage"; }
    @Override public String getDescription() { return "Shows RAM usage of the game."; }
    @Override public String getCategory() { return "HUD"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

package net.everlastingness.client.modules.performance;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Fog module — removes or reduces distance fog, mirroring Lunar's Fog mod.
 */
public class FogModule extends AbstractModule {
    @Override public String getId() { return "fog"; }
    @Override public String getName() { return "Fog"; }
    @Override public String getDescription() { return "Removes or reduces distance fog."; }
    @Override public String getCategory() { return "PERFORMANCE"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

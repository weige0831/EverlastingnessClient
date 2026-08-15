package net.everlastingness.client.modules.combat;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Damage Tint module — flashes the screen red when you take damage,
 * mirroring Lunar's DamageTint mod.
 */
public class DamageTintModule extends AbstractModule {
    @Override public String getId() { return "damage_tint"; }
    @Override public String getName() { return "Damage Tint"; }
    @Override public String getDescription() { return "Tints the screen when damaged."; }
    @Override public String getCategory() { return "COMBAT"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

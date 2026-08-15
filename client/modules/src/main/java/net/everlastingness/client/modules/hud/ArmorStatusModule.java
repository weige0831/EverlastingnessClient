package net.everlastingness.client.modules.hud;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Armor Status module — shows the durability of equipped armor and held item
 * with per-tier colors, mirroring Lunar's ArmorStatus mod.
 */
public class ArmorStatusModule extends AbstractModule {
    @Override public String getId() { return "armor_status"; }
    @Override public String getName() { return "Armor Status"; }
    @Override public String getDescription() { return "Shows armor and held item durability."; }
    @Override public String getCategory() { return "HUD"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

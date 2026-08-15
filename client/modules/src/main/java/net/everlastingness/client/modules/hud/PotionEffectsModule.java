package net.everlastingness.client.modules.hud;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Potion Effects module — shows active potion effects with remaining
 * duration bars, mirroring Lunar's PotionEffects mod.
 */
public class PotionEffectsModule extends AbstractModule {
    @Override public String getId() { return "potion_effects"; }
    @Override public String getName() { return "Potion Effects"; }
    @Override public String getDescription() { return "Shows active potion effects and durations."; }
    @Override public String getCategory() { return "HUD"; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

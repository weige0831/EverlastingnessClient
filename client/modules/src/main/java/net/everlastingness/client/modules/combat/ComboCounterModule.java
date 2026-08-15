package net.everlastingness.client.modules.combat;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Combo Counter module — counts consecutive hits landed without being hit,
 * mirroring Lunar's Combo mod.
 */
public class ComboCounterModule extends AbstractModule {
    private int combo = 0;

    @Override public String getId() { return "combo_counter"; }
    @Override public String getName() { return "Combo Counter"; }
    @Override public String getDescription() { return "Counts your consecutive hits."; }
    @Override public String getCategory() { return "COMBAT"; }

    public void onHit() { combo++; }
    public void onHurt() { combo = 0; }
    public int getCombo() { return combo; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

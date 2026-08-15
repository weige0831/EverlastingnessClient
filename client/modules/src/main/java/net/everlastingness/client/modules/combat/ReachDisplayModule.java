package net.everlastingness.client.modules.combat;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Reach Display module — shows the distance of your last successful hit,
 * mirroring Lunar's ReachDisplay mod.
 */
public class ReachDisplayModule extends AbstractModule {
    private String lastReach = "";

    @Override public String getId() { return "reach_display"; }
    @Override public String getName() { return "Reach Display"; }
    @Override public String getDescription() { return "Shows the distance of your last hit."; }
    @Override public String getCategory() { return "COMBAT"; }

    public void recordHit(double distance) {
        lastReach = String.format("%.2f", distance) + "m";
    }

    public String getLastReach() { return lastReach; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

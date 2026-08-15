package net.everlastingness.client.modules.utility;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Scoreboard module — scoreboard customization: hide the red objective
 * numbers, toggle the sidebar entirely, mirroring Lunar's Scoreboard mod.
 */
public class ScoreboardModule extends AbstractModule {
    private boolean hideRedNumbers = true;
    private boolean hideSidebar = false;

    @Override public String getId() { return "scoreboard"; }
    @Override public String getName() { return "Scoreboard"; }
    @Override public String getDescription() { return "Customizes the scoreboard sidebar."; }
    @Override public String getCategory() { return "UTILITY"; }

    public boolean isHidingRedNumbers() { return hideRedNumbers; }
    public boolean isHidingSidebar() { return hideSidebar; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

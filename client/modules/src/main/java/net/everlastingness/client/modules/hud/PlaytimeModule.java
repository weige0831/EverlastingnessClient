package net.everlastingness.client.modules.hud;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Playtime module — tracks how long the current session/world has been open,
 * mirroring Lunar's Playtime mod.
 */
public class PlaytimeModule extends AbstractModule {
    private long startedAt = System.currentTimeMillis();

    @Override public String getId() { return "playtime"; }
    @Override public String getName() { return "Playtime"; }
    @Override public String getDescription() { return "Shows your current playtime."; }
    @Override public String getCategory() { return "HUD"; }

    /** Elapsed session time formatted as H:MM:SS. */
    public String getElapsed() {
        long s = (System.currentTimeMillis() - startedAt) / 1000;
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

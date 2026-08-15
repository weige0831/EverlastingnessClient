package net.everlastingness.client.modules.visual;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Time Changer module — client-side time-of-day override, mirroring Lunar's
 * TimeChanger mod. Does not affect the server.
 */
public class TimeChangerModule extends AbstractModule {
    /** 0..24000 world time to display; -1 = follow server. */
    private long fixedTime = 6000; // noon

    @Override public String getId() { return "time_changer"; }
    @Override public String getName() { return "Time Changer"; }
    @Override public String getDescription() { return "Overrides client-side time of day."; }
    @Override public String getCategory() { return "VISUAL"; }

    public long getFixedTime() { return fixedTime; }
    public void setFixedTime(long t) { this.fixedTime = t; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

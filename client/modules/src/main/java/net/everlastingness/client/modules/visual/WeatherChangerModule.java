package net.everlastingness.client.modules.visual;

import net.everlastingness.client.common.module.AbstractModule;

/**
 * Weather Changer module — client-side weather override (always clear /
 * always rain), mirroring Lunar's WeatherChanger mod.
 */
public class WeatherChangerModule extends AbstractModule {
    public enum Mode { SERVER, CLEAR, RAIN }
    private Mode mode = Mode.CLEAR;

    @Override public String getId() { return "weather_changer"; }
    @Override public String getName() { return "Weather Changer"; }
    @Override public String getDescription() { return "Overrides client-side weather."; }
    @Override public String getCategory() { return "VISUAL"; }

    public Mode getMode() { return mode; }
    public void setMode(Mode m) { this.mode = m; }

    @Override public void onEnable() { }
    @Override public void onDisable() { }
}

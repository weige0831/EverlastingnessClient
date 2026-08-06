package net.everlastingness.client.modules.visual;

import net.everlastingness.client.common.module.AbstractModule;

import java.util.logging.Logger;

/**
 * Custom crosshair module — replaces the vanilla crosshair with a
 * customizable one (dot, cross, circle, etc.) with configurable color and
 * size. This is a key visual difference from vanilla that Lunar/Badlion users
 * immediately notice.
 *
 * <p>On MC 1.7.10, the per-version mixin (MixinGuiIngameCustomCrosshair)
 * injects at the drawCrosshair method and, if enabled, cancels the vanilla
 * crosshair draw and draws a custom one using OpenGL lines or a texture.</p>
 */
public class CustomCrosshairModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Crosshair");

    /** Crosshair style: 0=dot, 1=cross, 2=circle, 3=plus */
    private int style = 1; // cross
    /** RGB color, default white. */
    private int color = 0xFFFFFFFF;
    /** Size in pixels. */
    private int size = 10;
    /** Dynamic color: turns red when pointing at an entity. */
    private boolean dynamicColor = true;

    @Override
    public String getId() { return "custom_crosshair"; }

    @Override
    public String getName() { return "Custom Crosshair"; }

    @Override
    public String getDescription() {
        return "Replace the vanilla crosshair with a custom one.";
    }

    @Override
    public String getCategory() { return "VISUAL"; }

    public int getStyle() { return style; }
    public void setStyle(int style) { this.style = style; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public boolean isDynamicColor() { return dynamicColor; }
    public void setDynamicColor(boolean dynamicColor) { this.dynamicColor = dynamicColor; }

    @Override
    public void onEnable() {
        LOGGER.info("Custom crosshair enabled (style=" + style + ")");
    }

    @Override
    public void onDisable() {
        LOGGER.info("Custom crosshair disabled");
    }
}

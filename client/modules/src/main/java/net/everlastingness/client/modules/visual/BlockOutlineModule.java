package net.everlastingness.client.modules.visual;

import net.everlastingness.client.common.module.AbstractModule;

import java.util.logging.Logger;

/**
 * Block outline / hit-box color module — changes the color and style of the
 * wireframe box drawn around the block the player is looking at. Lunar and
 * Badlion both offer this with rainbow, solid, and distance-based color modes.
 */
public class BlockOutlineModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Outline");

    /** 0=rainbow, 1=solid, 2=distance-based */
    private int colorMode = 0; // rainbow
    /** Solid color (when mode=1), default cyan. */
    private int solidColor = 0xFF00FFFF;
    /** Line thickness, 1-4. */
    private int thickness = 2;

    @Override
    public String getId() { return "block_outline"; }

    @Override
    public String getName() { return "Block Outline"; }

    @Override
    public String getDescription() {
        return "Custom color and style for the block selection wireframe.";
    }

    @Override
    public String getCategory() { return "VISUAL"; }

    public int getColorMode() { return colorMode; }
    public int getSolidColor() { return solidColor; }
    public int getThickness() { return thickness; }

    /**
     * Compute the current outline color based on the color mode. Called by
     * the render mixin each frame.
     * @param distance Distance to the targeted block (for distance-based mode)
     * @return ARGB color int.
     */
    public int computeColor(double distance) {
        switch (colorMode) {
            case 1: return solidColor;
            case 2: {
                // Distance-based: green near, red far
                float t = (float) Math.min(1.0, distance / 6.0);
                int r = (int)(255 * t);
                int g = (int)(255 * (1.0 - t));
                return 0xFF000000 | (r << 16) | (g << 8);
            }
            default: {
                // Rainbow
                float hue = (System.currentTimeMillis() % 2000) / 2000.0f;
                java.awt.Color hsb = java.awt.Color.getHSBColor(hue, 1.0f, 1.0f);
                return 0xFF000000 | (hsb.getRed() << 16) | (hsb.getGreen() << 8) | hsb.getBlue();
            }
        }
    }

    @Override
    public void onEnable() { LOGGER.info("Block outline enabled"); }
    @Override
    public void onDisable() { LOGGER.info("Block outline disabled"); }
}

package net.everlastingness.client.modules.utility;

import net.everlastingness.client.common.module.AbstractModule;

import java.util.logging.Logger;

/**
 * Coordinate Copy / Send module — allows one-click copying of current
 * coordinates to clipboard or sending them in chat. This is a signature
 * Lunar/Badlion QoL feature used heavily in PvP and faction servers.
 */
public class CoordinateCopyModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Coords");

    /** Last known player position (updated by mixin each tick). */
    private double posX, posY, posZ;
    private String dimension = "overworld";

    /** Copy format: "x, y, z" or "[x, y, z]" */
    private String format = "[x, y, z]";

    @Override
    public String getId() { return "coord_copy"; }

    @Override
    public String getName() { return "Coordinate Copy"; }

    @Override
    public String getDescription() {
        return "Copy or send your coordinates with one keybind.";
    }

    @Override
    public String getCategory() { return "UTILITY"; }

    public void updatePosition(double x, double y, double z) {
        this.posX = x; this.posY = y; this.posZ = z;
    }

    public void setDimension(String dim) { this.dimension = dim; }

    /** Format the current position as a copyable string. */
    public String getFormattedCoords() {
        return format
            .replace("x", String.format("%.0f", posX))
            .replace("y", String.format("%.0f", posY))
            .replace("z", String.format("%.0f", posZ));
    }

    /**
     * Copy to clipboard (called by keybind mixin).
     * Uses java.awt.datatransfer.
     */
    public void copyToClipboard() {
        String text = getFormattedCoords();
        try {
            java.awt.Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(text), null);
            LOGGER.info("Coordinates copied: " + text);
        } catch (Throwable t) {
            LOGGER.warning("Clipboard copy failed: " + t.getMessage());
        }
    }

    @Override
    public void onEnable() { LOGGER.info("Coordinate copy enabled"); }
    @Override
    public void onDisable() { LOGGER.info("Coordinate copy disabled"); }
}

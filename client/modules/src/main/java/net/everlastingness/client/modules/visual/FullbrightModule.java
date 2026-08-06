package net.everlastingness.client.modules.visual;

import net.everlastingness.client.common.module.AbstractModule;

import java.util.logging.Logger;

/**
 * Fullbright / night-vision module — eliminates darkness in caves and at
 * night by setting the game's brightness/partial-tick lightmap to maximum.
 * This is one of Lunar/Badlion's most commonly used visual features.
 *
 * <p>On MC 1.7.10, the per-version mixin (e.g. MixinEntityRendererFullbright)
 * injects at the HEAD of updateLightmap() and, if this module is enabled,
 * sets all RGB values in the lightmap texture to maximum (255), overriding
 * the darkness calculation. This produces full-brightness rendering without
 * Gamma override hacks.</p>
 */
public class FullbrightModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Fullbright");

    @Override
    public String getId() { return "fullbright"; }

    @Override
    public String getName() { return "Fullbright"; }

    @Override
    public String getDescription() {
        return "Removes darkness — full brightness in caves and at night.";
    }

    @Override
    public String getCategory() { return "VISUAL"; }

    @Override
    public void onEnable() {
        LOGGER.info("Fullbright enabled");
    }

    @Override
    public void onDisable() {
        LOGGER.info("Fullbright disabled — lightmap restored");
    }
}

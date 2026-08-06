package net.everlastingness.client.modules.cape;

import net.everlastingness.client.common.cosmetics.CosmeticsRegistry;
import net.everlastingness.client.common.module.AbstractModule;

import java.util.logging.Logger;

/**
 * The Everlastingness cape cosmetics module.
 *
 * <p>When enabled, the per-version cape render mixin ({@code MixinRenderPlayerCape}
 * on 1.7.10) draws the configured custom cape for each registered player. This
 * module owns the on/off flag surfaced in the config GUI and toggles the global
 * {@link CosmeticsRegistry#setEnabled(boolean)} flag the mixin reads.</p>
 */
public class CapeModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/CUD"); // Cosmetics

    @Override
    public String getId() {
        return "cape";
    }

    @Override
    public String getName() {
        return "Cape (Cosmetics)";
    }

    @Override
    public String getDescription() {
        return "Renders a custom Everlastingness cape for registered players.";
    }

    @Override
    public String getCategory() {
        return "cosmetics";
    }

    @Override
    public void onEnable() {
        CosmeticsRegistry.get().setEnabled(true);
        LOGGER.info("Cape cosmetics enabled");
    }

    @Override
    public void onDisable() {
        CosmeticsRegistry.get().setEnabled(false);
        LOGGER.info("Cape cosmetics disabled");
    }
}

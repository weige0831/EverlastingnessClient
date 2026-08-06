package net.everlastingness.client.modules.fps;

import net.everlastingness.client.common.fps.EntityCullConfig;
import net.everlastingness.client.common.module.AbstractModule;

import java.util.logging.Logger;

/**
 * The Everlastingness FPS optimization module.
 *
 * <p>When enabled, the per-version entity-cull mixin ({@code MixinEntityCull} on
 * 1.7.10) skips rendering entities beyond the configured cull distance, cutting
 * per-frame render cost for crowded/distant-entity scenes. This is the same
 * class of optimisation Sodium/Lithium apply to entity rendering.</p>
 *
 * <p>The module surfaces the on/off flag in the config GUI and drives the global
 * {@link EntityCullConfig}.</p>
 */
public class FpsOptimizationModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/FPS");

    @Override
    public String getId() {
        return "fps_boost";
    }

    @Override
    public String getName() {
        return "FPS Boost (Entity Cull)";
    }

    @Override
    public String getDescription() {
        return "Skips rendering entities beyond the cull distance to raise FPS.";
    }

    @Override
    public String getCategory() {
        return "performance";
    }

    @Override
    public boolean isEnabledByDefault() {
        // Opt-in: culling changes what's visible, so leave it off until the user
        // turns it on from the config GUI.
        return false;
    }

    @Override
    public void onEnable() {
        EntityCullConfig config = EntityCullConfig.get();
        config.setCullDistance(EntityCullConfig.DEFAULT_CULL_DISTANCE);
        config.setEnabled(true);
        LOGGER.info("FPS boost enabled (entity cull @ " + config.getCullDistance() + " blocks)");
    }

    @Override
    public void onDisable() {
        EntityCullConfig.get().setEnabled(false);
        LOGGER.info("FPS boost disabled");
    }
}

package net.everlastingness.client.modules.hud;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.event.RenderTickEvent;
import net.everlastingness.client.common.module.AbstractModule;

import java.util.logging.Logger;

/**
 * The Everlastingness HUD module. Draws a small persistent overlay showing
 * that the client is loaded, plus the running Minecraft version.
 *
 * <p>This is the canonical Phase 0 "vertical slice" module: it proves the
 * module lifecycle and event bus work end-to-end. The actual on-screen drawing
 * is delegated to a per-version renderer adapter (implemented in each version
 * subproject) because the font/Gui API differs between MC versions.</p>
 */
public class HudOverlayModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/HUD");

    private volatile int framesRendered;

    @Override
    public String getId() {
        return "hud";
    }

    @Override
    public String getName() {
        return "HUD Overlay";
    }

    @Override
    public String getDescription() {
        return "Displays a persistent overlay confirming Everlastingness is active.";
    }

    @Override
    public String getCategory() {
        return "hud";
    }

    @Override
    public void onEnable() {
        EverlastingnessClient.get().events().subscribe(RenderTickEvent.class, this::onRenderTick);
        LOGGER.info("HUD overlay enabled — listening for render ticks");
    }

    @Override
    public void onDisable() {
        EverlastingnessClient.get().events().unsubscribeAll(this);
        LOGGER.info("HUD overlay disabled");
    }

    private void onRenderTick(RenderTickEvent event) {
        framesRendered++;
        // The per-version mixin/renderer reads this counter and the client
        // singleton to draw the overlay text each frame.
    }

    /** Frame counter for display; read by the per-version renderer. */
    public int framesRendered() {
        return framesRendered;
    }
}

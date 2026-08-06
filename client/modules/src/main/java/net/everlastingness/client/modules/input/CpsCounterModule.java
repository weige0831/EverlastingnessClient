package net.everlastingness.client.modules.input;

import net.everlastingness.client.common.module.AbstractModule;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * CPS (Clicks Per Second) counter module — one of Lunar/Badlion's most
 * visible HUD features. Tracks left and right mouse clicks per second
 * using a rolling-window algorithm.
 *
 * <p>On MC 1.7.10, the per-version mixin (MixinMinecraftRunTick or a
 * dedicated click mixin) calls {@link #onLeftClick()} and
 * {@link #onRightClick()} from Minecraft's click handlers. This class
 * computes CPS from a 1-second rolling window and stores the result for
 * the HUD renderer to display.</p>
 */
public class CpsCounterModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/CPS");

    /** Click timestamps within the last 1000ms. */
    private final List<Long> leftClicks = new ArrayList<>();
    private final List<Long> rightClicks = new ArrayList<>();

    @Override
    public String getId() { return "cps_counter"; }

    @Override
    public String getName() { return "CPS Counter"; }

    @Override
    public String getDescription() {
        return "Displays left and right clicks per second in the HUD.";
    }

    @Override
    public String getCategory() { return "INPUT"; }

    /** Called by the click mixin when the left mouse button is pressed. */
    public void onLeftClick() {
        leftClicks.add(System.currentTimeMillis());
    }

    /** Called by the click mixin when the right mouse button is pressed. */
    public void onRightClick() {
        rightClicks.add(System.currentTimeMillis());
    }

    /** Prune clicks older than 1 second. Called every tick by the HUD. */
    public void prune() {
        long now = System.currentTimeMillis();
        leftClicks.removeIf(t -> now - t > 1000);
        rightClicks.removeIf(t -> now - t > 1000);
    }

    /** Current left CPS (clicks in the last 1000ms). */
    public int getLeftCps() {
        prune();
        return leftClicks.size();
    }

    /** Current right CPS. */
    public int getRightCps() {
        prune();
        return rightClicks.size();
    }

    @Override
    public void onEnable() {
        LOGGER.info("CPS counter enabled");
    }

    @Override
    public void onDisable() {
        leftClicks.clear();
        rightClicks.clear();
        LOGGER.info("CPS counter disabled");
    }
}

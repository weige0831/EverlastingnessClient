package net.everlastingness.client.common.module;

/**
 * Contract for an Everlastingness feature module. A module owns a slice of
 * client functionality (e.g. HUD overlay, FPS optimization, keybind manager)
 * and is loaded by the {@code EverlastingnessClient} bootstrap once Mixin is
 * up.
 *
 * <p>Modules live in the version-agnostic {@code :modules} project so the same
 * feature code is reused across every supported Minecraft version; only the
 * Mixins that hook the feature into a specific MC version differ.</p>
 */
public interface Module {

    /** Stable, unique id used in config files and commands, e.g. {@code "fps_boost"}. */
    String getId();

    /** Human-readable name shown in the in-game module list. */
    String getName();

    /** Human-readable description. */
    default String getDescription() {
        return "";
    }

    /**
     * The Minecraft version-agnostic category this module belongs to, e.g.
     * {@code "render"}, {@code "hud"}, {@code "utility"}.
     */
    default String getCategory() {
        return "misc";
    }

    /** Whether the module is enabled at boot (config may override). */
    default boolean isEnabledByDefault() {
        return true;
    }

    /** Whether the module is currently enabled. */
    boolean isEnabled();

    /** Transition this module to enabled (idempotent). */
    void enable();

    /** Transition this module to disabled (idempotent). */
    void disable();

    /** Called once when the module is loaded and enabled. */
    void onEnable();

    /** Called when the module is disabled (also called on shutdown). */
    void onDisable();
}

package net.everlastingness.client.common.module;

/**
 * Module priority levels, matching the visual impact categories used by
 * Lunar/Badlion for feature comparison.
 */
public enum ModuleCategory {
    /** Always-visible HUD elements (coordinates, FPS, CPS, clock, armor). */
    HUD,
    /** Input-related (keybinds, CPS counter, scroll optimization). */
    INPUT,
    /** Visual rendering tweaks (crosshair, brightness, damage flash). */
    VISUAL,
    /** Camera/view modifications (perspective toggle, zoom, free-look). */
    CAMERA,
    /** Chat enhancements (copy, search, screenshot). */
    CHAT,
    /** Performance optimizations (entity culling, chunk caching). */
    PERFORMANCE,
    /** Cosmetics (capes, wings, particles). */
    COSMETICS,
    /** Utility/QoL (auto-reconnect, ping display, server info). */
    UTILITY
}

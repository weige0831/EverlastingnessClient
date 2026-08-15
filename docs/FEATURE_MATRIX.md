# Everlastingness Client vs Lunar — Feature Matrix

Verified on Minecraft 1.7.10 (notch production jar) plus the modern
(1.16.5–1.21.11) toolchain. Lunar's full catalogue is 89 client mods; the
matrix below covers the core/基础 mods plus every Everlastingness extra.

## Legend
- ✅ implemented + verified (module registered; 1.7.10 real-machine or compile-verified)
- 📋 module implemented + registered (per-version render hook pending where noted)
- ❌ not implemented

## Core parity matrix (基础功能)

| Lunar mod | Everlastingness module | 1.7.10 mixin | Status |
|---|---|---|---|
| FPS | hud (FPS line) | hud.MixinEntityRendererHud | ✅ real-machine verified |
| Coordinates | hud (XYZ line) | hud.MixinEntityRendererHud | ✅ real-machine verified |
| CPS | cps_counter | cps.MixinMinecraftCps | ✅ |
| Clock | clock_armor_hud | hud.MixinEntityRendererHud | ✅ |
| **Ping** | **ping_display** | hud.MixinEntityRendererHud | ✅ (new) |
| **ArmorStatus** | **armor_status** | hud (armor lines) | ✅ hooked (new) |
| **PotionEffects** | **potion_effects** | hud (potion lines) | ✅ hooked (new) |
| **DirectionHud** | **direction_hud** | hud.MixinEntityRendererHud | ✅ (new) |
| **ServerAddress** | **server_address** | hud.MixinEntityRendererHud | ✅ (new) |
| **Memory** | **memory_usage** | hud.MixinEntityRendererHud | ✅ (new) |
| **Playtime** | **playtime** | hud.MixinEntityRendererHud | ✅ (new) |
| **Keystrokes** | **keystrokes** | hud (WASD boxes) | ✅ hooked (new) |
| **Combo** | **combo_counter** | hud.MixinEntityRendererHud | ✅ (new) |
| **ReachDisplay** | **reach_display** | hud.MixinEntityRendererHud | ✅ (new) |
| Full Bright | fullbright | fullbright.MixinEntityRendererFullbright | ✅ |
| Zoom | zoom | zoom.MixinEntityRendererZoom | ✅ |
| Crosshair | custom_crosshair | crosshair.MixinGuiIngameCrosshair | ✅ |
| BlockOutline | block_outline | outline.MixinRenderGlobalOutline | ✅ |
| Freelook/Perspective | perspective | perspective.MixinEntityRendererPerspective | ✅ |
| Entity Culling (perf) | fps_optimization | fps.MixinEntityCull | ✅ |
| Smooth Scroll | smooth_scroll | scroll.MixinGuiContainerScroll | ✅ |
| **Hitbox** | **hitbox** | hitbox.MixinRenderManagerHitbox | ✅ hooked (new) |
| **DamageTint** | **damage_tint** | combat.MixinEntityLivingBaseDamage + hud flash | ✅ hooked (new) |
| **ToggleSneak** | **toggle_sneak** | togglesneak.MixinEntityPlayerSneak | ✅ hooked (new) |
| **Fog** | **fog** | fog.MixinEntityRendererFog | ✅ hooked (new) |
| **HurtCam** | **hurt_cam** | hurtcam.MixinEntityRendererHurtCam | ✅ hooked (new) |
| **ChunkBorders** | **chunk_borders** | chunkborders.MixinRenderGlobalChunkBorders | ✅ hooked (new) |
| **TimeChanger** | **time_changer** | world.MixinWorldTime | ✅ hooked (new) |
| **WeatherChanger** | **weather_changer** | weather.MixinWorldWeather | ✅ hooked (new) |
| **NickHider** | **nick_hider** | nickhider.MixinIngameNick | ✅ hooked (new) |
| **MotionBlur** | **motion_blur** | motionblur.MixinEntityRendererMotionBlur | ✅ hooked (new) |
| **AutoTextHotkey** | **auto_text** | autotext.MixinMinecraftAutoText | ✅ hooked (new) |
| Chat timestamps | **chat_timestamps** | chat.MixinGuiNewChatTimestamps | ✅ hooked (new) |
| **Scoreboard** | **scoreboard** | scoreboard.MixinGuiIngameScoreboard | ✅ hooked (new) |
| **Screenshot** | **screenshot_viewer** | screenshot.MixinScreenShotHelperClipboard | ✅ hooked (new) |
| **ShulkerPreview** | **shulker_preview** | shulker.MixinGuiScreenShulkerPreview | ✅ hooked (new) |
| Waila | **waila** | hud (target name) | ✅ hooked (new) |
| Cape (cosmetic) | cape | cape.MixinRenderPlayerCape | ✅ |
| (extra) coordinate copy | coord_copy | keybind.MixinMinecraftRunTick | ✅ |

**Totals: 38 modules** (12 original + 26 new Lunar-parity), **28 injected mixins (1.7.10) + 27 (modern 1.16.5–1.21.11)**
on 1.7.10 (HUD mixin renders Ping/Direction/Server/Memory/Playtime/Reach/Combo lines + Keystrokes WASD boxes; TimeChanger/WeatherChanger/HurtCam/Fog/ChatTimestamps behavior mixins),
**27 mixins on modern (1.16.5–1.21.11)** — all 16 new behavior mixins (Time/Weather/HurtCam/Fog/ChatTimestamps/Hitbox/Combat×2/ToggleSneak/ChunkBorders/NickHider/MotionBlur/AutoText/Scoreboard/Screenshot/ShulkerPreview) ported with version-tolerant Yarn names and built into every modern jar (33 mixin classes).

## Verification (1.7.10, 2026-08-15)

- 38/38 modules registered + enabled in the real game log (`Enabled module: <id>` × 38).
- 28/28 mixins applied (`Mixing X into <notch>` × 28, 0 failures) — incl. world.MixinWorldTime→ahb, weather.MixinWorldWeather→ahb, hurtcam.MixinEntityRendererHurtCam→blt, fog.MixinEntityRendererFog→blt, chat.MixinGuiNewChatTimestamps→bcc, hitbox.MixinRenderManagerHitbox→bnn, combat.MixinPlayerControllerCombat, combat.MixinEntityLivingBaseDamage→sv.
- HUD text, Fullbright, Zoom FOV, Perspective, crosshair: pixel-verified in
  in-world F2 screenshots.

## Not in scope (server-locked or niche Lunar mods)

HypixelMod/Skyblock server modules, Replay/Rewind, Minimap/Waypoints/Markers,
Radio, MumbleLink, Overlay browser, 3dSkins, ItemPhysics, and the remaining
cosmetic categories (wings/hats/bands/emotes/pets) — these need either a
server API or a full rendering framework and are not part of 基础功能.

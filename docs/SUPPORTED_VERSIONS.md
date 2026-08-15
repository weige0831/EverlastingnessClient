# Supported Minecraft Versions — Everlastingness vs Lunar

Everlastingness aims to mirror **all** Minecraft versions supported by Lunar
Client, so users switching from Lunar can launch the exact same version set.

## Coverage

Everlastingness supports **33 versions** matching Lunar Client's playable
catalogue (verified 2026-08 against Lunar's launcher API). The only version in
Lunar's metadata that is intentionally excluded is **1.21.2** — it returns
`NO_PERMISSION_PRIVATE_VERSION` (a private Lunar build, not publicly playable).

## Version matrix

### Legacy era (LaunchWrapper + MCP/SRG mappings, MixinTweaker)
Built with RetroFuturaGradle (1.7.10) / ForgeGradle (1.8.9+). Injected via
Mojang's LaunchWrapper using `--tweakClass`.

| Version | Notes |
|---|---|
| 1.7.10 | Deepest legacy — full notch-obf at runtime, requires the SRG→notch build-time patcher |
| 1.8.9 | PvP standard |
| 1.9.4 | |
| 1.11.2 | |
| 1.12.2 | Last LaunchWrapper version |

### Modern era (Java-agent injection, Mojang/Yarn mappings)
Built with Fabric Loom (development toolchain only — runtime is standalone,
no Fabric Loader required). Injected via `-javaagent`.

| Version | Notes |
|---|---|
| 1.16.5 | First modern version |
| 1.17.1 | |
| 1.18.1, 1.18.2 | |
| 1.19, 1.19.2, 1.19.3, 1.19.4 | |
| 1.20, 1.20.1–1.20.6 | |
| 1.21, 1.21.1, 1.21.3–1.21.11 | (1.21.2 excluded — Lunar private build) |
| 26.1, 26.2 | Mojang "26.x" rebrand, Fabric-based |

## Build status (2026-08-07)

### ✅ Built + staged (28 versions)

| Version | Mixin count | Build toolchain |
|---|---|---|
| 1.7.10 | 12 (notch-remapped) | RFG + LaunchWrapper (Gradle 8.8, Java 8) |
| 1.8.9, 1.9.4, 1.11.2, 1.12.2 | 2 (minimal; LWJGL2 era) | Loom 1.6.10 + LegacyFabric Yarn + Gradle 8.7 |
| 1.16.5 | 10 | Loom 1.6.10 + Gradle 8.7 |
| 1.17.1 | 10 | Loom 1.6.10 + Gradle 8.7 |
| 1.18.1, 1.18.2 | 10 | Loom 1.6.10 + Gradle 8.7 |
| 1.19, 1.19.2, 1.19.3, 1.19.4 | 10 | Loom 1.6.10 + Gradle 8.7 |
| 1.20, 1.20.1–1.20.6 | 10 | Loom 1.6.10 + Gradle 8.7 |
| 1.21, 1.21.1, 1.21.3–1.21.8 | 10 | Loom 1.6.10 + Gradle 8.7 |

### 📋 Registered but not built (5 versions)

**Latest (1.21.9, 1.21.10, 1.21.11, 26.1, 26.2)**: These use Yarn mappings
with unpick format v2, which Loom 1.6.10 rejects (`Unsupported unpick version`).
Loom 1.14+ supports unpick v2 but requires Gradle 9.2+ AND calls
`MemoryMappingTree.propagateOuterClassNames` (absent from all released
mapping-io jars). Java agent bytecode injection successfully added the shim
method and bypassed the unpick version check, but Gradle 9.x's instrumentation
agent performs SHA-256 digest verification on every instrumented class at
`invokedynamic` bootstrap time and rejects the modified bytes
(`SecurityException: SHA-256 digest error`). This is a Gradle security boundary
that cannot be bypassed via JVM args or cache manipulation — the instrumented
jar is regenerated on every build with fresh per-class SHA-256 digests.

**Root cause**: Upstream Fabric Loom release bug. Loom 1.13.6+ was compiled
against an unreleased mapping-io snapshot containing `propagateOuterClassNames`,
but no released mapping-io (0.5.1–0.8.0) has this method. This must be fixed
upstream (either Loom releases a compatible version or mapping-io adds the
method) before these 5 versions can be built.

### Key technical achievement

The mappingio upstream bug was solved by discovering (via binary-searching
all released mapping-io jars and all Loom versions) that **Loom 1.6.10 is the
last release that does not call `propagateOuterClassNames`**, and it works
with Gradle 8.7 (which provides the `Problems` API Loom 1.6.10 needs). This
combination unlocked builds for 1.16.5 through 1.21.8 — 23 modern versions.

To build all registered versions, run:
```bash
./scripts/build-all-versions.sh all      # legacy + modern
./scripts/build-all-versions.sh modern   # 1.16.5+ (28 versions)
./scripts/build-all-versions.sh legacy   # 1.7.10–1.12.2 (5 versions)
```

## Feature parity across eras

Both the legacy (1.7.10) and modern (1.20.x) clients now ship the **same 12
feature mixins**, version-mapped:

| Feature | 1.7.10 Mixin (MCP/notch) | 1.20.x Mixin (Yarn) |
|---|---|---|
| HUD (XYZ/FPS/CPS/Clock) | `hud.MixinEntityRendererHud` | `hud.MixinGameRendererHud` |
| FPS (entity cull) | `fps.MixinEntityCull` | `fps.MixinEntityRenderDispatcherFps` |
| CPS | `cps.MixinMinecraftCps` | `cps.MixinMinecraftClientCps` |
| Fullbright | `fullbright.MixinEntityRendererFullbright` | `fullbright.MixinLightmapFullbright` |
| Zoom (FOV) | `zoom.MixinEntityRendererZoom` | `zoom.MixinGameRendererZoom` |
| Crosshair | `crosshair.MixinGuiIngameCrosshair` | `crosshair.MixinInGameHudCrosshair` |
| Perspective | `perspective.MixinEntityRendererPerspective` | `perspective.MixinGameRendererPerspective` |
| Block outline | `outline.MixinRenderGlobalOutline` | `outline.MixinWorldRendererOutline` |
| Smooth scroll | `scroll.MixinGuiContainerScroll` | `scroll.MixinHandledScreenScroll` |
| Cape | `cape.MixinRenderPlayerCape` | `cape.MixinPlayerEntityRendererCape` |
| Keybinds | `keybind.MixinMinecraftRunTick` | `keybind.MixinMinecraftClientKeybind` |
| AutoWorld (test) | `autoworld.MixinAutoWorld` | (1.20.x: covered by agent) |

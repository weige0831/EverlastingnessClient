using System.Collections.Generic;

namespace Everlastingness.Launcher.Core.Clients;

/// <summary>
/// Describes how the Everlastingness client is injected into a single
/// Minecraft version. Mirrors the per-version approach used by Lunar/Badlion:
/// each MC version has its own compiled client jar plus the right injection
/// mechanism for its era (LaunchWrapper for legacy, ModLauncher/agent for modern).
/// </summary>
public sealed class EverlastingnessClientProfile
{
    /// <summary>Minecraft version id, e.g. "1.8.9", "1.20.4".</summary>
    public required string MinecraftVersion { get; init; }

    /// <summary>
    /// Mapping era this version belongs to. Determines which Gradle toolchain
    /// built the client jar and which injection bootstrap the launcher uses.
    /// </summary>
    public required MappingEra Era { get; init; }

    /// <summary>
    /// Name of the compiled Everlastingness client jar for this version, e.g.
    /// "everlastingness-1.8.9.jar". The launcher resolves this against the
    /// <see cref="IClientAssetsLocator.ClientDirectory"/>.
    /// </summary>
    public required string ClientJar { get; init; }

    /// <summary>
    /// Extra classpath entries (besides the vanilla jar) required to inject.
    /// For legacy versions this is launchwrapper + mixin; for modern versions
    /// it is modlauncher + mixin. Resolved relative to the client directory.
    /// </summary>
    public IReadOnlyList<string> ExtraClasspath { get; init; } = [];

    /// <summary>
    /// Tweak classes passed via <c>--tweakClass</c> (LaunchWrapper era) or the
    /// equivalent transformer entry for modern era.
    /// </summary>
    public IReadOnlyList<string> TweakClasses { get; init; } = [];

    /// <summary>
    /// JVM system properties set when launching with the client injected,
    /// e.g. <c>mixin.configs</c>, <c>mixin.debug</c>.
    /// </summary>
    public IReadOnlyDictionary<string, string> SystemProperties { get; init; } = new Dictionary<string, string>();

    /// <summary>
    /// Main class used to launch. For the LaunchWrapper era this is
    /// <c>net.minecraft.launchwrapper.Launch</c>; for vanilla without injection
    /// it is <c>net.minecraft.client.main.Main</c>.
    /// </summary>
    public string MainClass { get; init; } = "net.minecraft.client.main.Main";
}

/// <summary>
/// The deobfuscation-mapping era a Minecraft version belongs to. Each era
/// needs a different build toolchain and runtime injection bootstrap.
/// </summary>
public enum MappingEra
{
    /// <summary>
    /// 1.7.10 / 1.8.9 / 1.12.2 — MCP mappings, LaunchWrapper injection,
    /// built with RetroFuturaGradle (1.7.10) or ForgeGradle 2.x (1.8.9+).
    /// </summary>
    LegacyLaunchWrapper,

    /// <summary>
    /// 1.16.5+ — Mojang/Yarn mappings, ModLauncher/agent injection, built
    /// with Fabric Loom (development toolchain only; runtime is standalone).
    /// </summary>
    ModernModLauncher,
}

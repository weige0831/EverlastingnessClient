using System.Collections.Generic;
using System.Linq;

namespace Everlastingness.Launcher.Core.Clients;

/// <summary>
/// The catalogue of Minecraft versions Everlastingness targets, mirroring the
/// version matrix Lunar Client supports. Each entry pins the injection
/// bootstrap appropriate for that version's era.
///
/// Why per-era injection differs:
///  - Legacy (1.7.10–1.12.2): built with MCP mappings, injected via Mojang's
///    LaunchWrapper using --tweakClass. Mixin bootstraps through MixinTweaker.
///  - Modern (1.16.5+): built with Mojang/Yarn mappings under Fabric Loom
///    (development toolchain only), injected at runtime via ModLauncher/agent.
/// </summary>
public static class ClientProfiles
{
    /// <summary>
    /// A legacy-era profile using LaunchWrapper + MixinTweaker injection.
    /// Shared by 1.7.10, 1.8.9 and 1.12.2; only the client jar name differs.
    /// </summary>
    private static EverlastingnessClientProfile Legacy(string mc, string jar) => new()
    {
        MinecraftVersion = mc,
        Era = MappingEra.LegacyLaunchWrapper,
        ClientJar = jar,
        // launchwrapper hosts the classloader; mixin provides MixinTweaker which
        // calls MixinBootstrap.start() and registers the mixin transformer.
        ExtraClasspath =
        [
            "launchwrapper-1.12.jar",
            "mixin-0.8.7.jar",
            jar
        ],
        TweakClasses =
        [
            "org.spongepowered.asm.launch.MixinTweaker",
            "net.everlastingness.client.tweaker.ClientTweaker"
        ],
        SystemProperties = new Dictionary<string, string>
        {
            ["mixin.configs"] = "mixins.everlastingness.json",
            ["mixin.env.remapRefMap"] = "true",
            ["everlastingness.version"] = mc
        },
        MainClass = "net.minecraft.launchwrapper.Launch"
    };

    /// <summary>
    /// A modern-era profile using ModLauncher/agent injection. The client jar
    /// is built with Fabric Loom (Mojang mappings) but launched standalone —
    /// Fabric Loader is NOT required at runtime.
    /// </summary>
    private static EverlastingnessClientProfile Modern(string mc, string jar) => new()
    {
        MinecraftVersion = mc,
        Era = MappingEra.ModernModLauncher,
        ClientJar = jar,
        ExtraClasspath =
        [
            "modlauncher-10.x.jar",
            "mixin-0.8.7.jar",
            jar
        ],
        TweakClasses = [],
        SystemProperties = new Dictionary<string, string>
        {
            ["mixin.configs"] = "mixins.everlastingness.json",
            ["everlastingness.version"] = mc
        },
        // 1.13+ vanilla still ships net.minecraft.client.main.Main as the entry
        // point; ModLauncher hooks in as a -javaagent before Main runs.
        MainClass = "net.minecraft.client.main.Main"
    };

    /// <summary>All supported version profiles, keyed by Minecraft version id.</summary>
    public static readonly IReadOnlyDictionary<string, EverlastingnessClientProfile> All =
        new Dictionary<string, EverlastingnessClientProfile>
        {
            ["1.7.10"] = Legacy("1.7.10", "everlastingness-1.7.10.jar"),
            ["1.8.9"] = Legacy("1.8.9", "everlastingness-1.8.9.jar"),
            ["1.12.2"] = Legacy("1.12.2", "everlastingness-1.12.2.jar"),
            ["1.16.5"] = Modern("1.16.5", "everlastingness-1.16.5.jar"),
            ["1.17.1"] = Modern("1.17.1", "everlastingness-1.17.1.jar"),
            ["1.18.2"] = Modern("1.18.2", "everlastingness-1.18.2.jar"),
            ["1.19.2"] = Modern("1.19.2", "everlastingness-1.19.2.jar"),
            ["1.19.4"] = Modern("1.19.4", "everlastingness-1.19.4.jar"),
            ["1.20.1"] = Modern("1.20.1", "everlastingness-1.20.1.jar"),
            ["1.20.4"] = Modern("1.20.4", "everlastingness-1.20.4.jar"),
            ["1.21"] = Modern("1.21", "everlastingness-1.21.jar"),
            ["1.21.4"] = Modern("1.21.4", "everlastingness-1.21.4.jar")
        };

    /// <summary>The Minecraft version ids Everlastingness can inject into.</summary>
    public static IEnumerable<string> SupportedVersions => All.Keys;

    /// <summary>Lookup a profile; null if this version isn't supported.</summary>
    public static EverlastingnessClientProfile? Find(string minecraftVersion) =>
        All.TryGetValue(minecraftVersion, out var p) ? p : null;

    /// <summary>
    /// Whether the client jar for a version is present in the client directory.
    /// </summary>
    public static bool IsArtifactReady(
        EverlastingnessClientProfile profile,
        IClientAssetsLocator locator) =>
        locator.ArtifactExists(profile.ClientJar);
}

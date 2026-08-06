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
        // ASM, Guava, Gson, joptsimple, log4j are Mixin runtime dependencies.
        ExtraClasspath =
        [
            "launchwrapper-1.12.jar",
            "mixin-0.8.7.jar",
            "asm-9.6.jar",
            "asm-tree-9.6.jar",
            "asm-commons-9.6.jar",
            "asm-util-9.6.jar",
            "guava-15.0.jar",
            "gson-2.2.4.jar",
            "jopt-simple-4.5.jar",
            "log4j-api-2.0-beta9.jar",
            "log4j-core-2.0-beta9.jar",
            jar
        ],
        TweakClasses =
        [
            // PreTweaker adds org.spongepowered.asm. classloader exclusion,
            // then MixinTweaker does the standard bootstrap (now ASM is on cp).
            // MixinTweaker reads --tweakClass args to find mixin configs.
            "net.everlastingness.client.v1_7_10.tweaker.EverlastingnessPreTweaker",
            "org.spongepowered.asm.launch.MixinTweaker",
            "net.everlastingness.client.v1_7_10.tweaker.ClientTweaker"
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
    /// A modern-era profile using the Java-agent injection path. The client jar
    /// is built with Fabric Loom (Yarn mappings) and its manifest declares
    /// <c>Premain-Class = ...EverlastingnessAgent</c>, so the launcher injects it
    /// with <c>-javaagent:&lt;jar&gt;</c>. Fabric Loader is NOT required at runtime.
    /// </summary>
    /// <remarks>
    /// Loom statically remaps the mixin bytecode to the <b>intermediary</b>
    /// namespace at build time (verified: <c>GameRenderer</c>→<c>class_757</c>,
    /// <c>render</c>→<c>method_3192</c>). Vanilla runtime classes, however, are
    /// <b>official/obfuscated</b> names. So before the agent jar actually applies
    /// mixins at runtime, the launcher (or a build step) must perform
    /// intermediary→official reobfuscation. This is tracked as the
    /// "intermediary→official reobf gap" — see README.
    /// </remarks>
    private static EverlastingnessClientProfile Modern(string mc, string jar) => new()
    {
        MinecraftVersion = mc,
        Era = MappingEra.ModernModLauncher,
        ClientJar = jar,
        // The agent jar bundles the client code + Premain-Class in its manifest.
        // Runtime Mixin support is supplied by these libs alongside it.
        ExtraClasspath =
        [
            "mixin-0.8.7.jar",
            jar
        ],
        TweakClasses = [],
        SystemProperties = new Dictionary<string, string>
        {
            ["mixin.configs"] = "everlastingness.mixins.json",
            ["everlastingness.version"] = mc
        },
        // 1.13+ vanilla still ships net.minecraft.client.main.Main as the entry
        // point; the agent hooks in via -javaagent before Main runs.
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

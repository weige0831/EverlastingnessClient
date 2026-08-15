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
            "asm-analysis-9.6.jar",
            "asm-commons-9.6.jar",
            "asm-util-9.6.jar",
            "guava-17.0.jar",
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
        MixinConfigs =
        [
            // MixinTweaker reads these from the command line (--mixin <name>).
            // The mixin.configs system property is ignored on 0.8.x.
            "mixins.everlastingness.json"
        ],
        SystemProperties = new Dictionary<string, string>
        {
            // All @Inject method names and @Shadow field names in the mixins
            // use SRG names directly (func_*, field_*) so no refmap remap is
            // needed. Build-time MixinTargetPatcher remaps all class references
            // to notch. Disabling the refmap avoids the searge/notch env
            // selection ambiguity that breaks @Inject selector parsing.
            ["mixin.env.disableRefMap"] = "true",
            // Verbose mixin logging so we can confirm each mixin APPLIED.
            ["mixin.debug"] = "true",
            // AutoWorld mixin: auto-create & join a singleplayer world on main
            // menu load, for headless in-world testing.
            ["everlastingness.version"] = mc,
            ["everlastingness.autoworld"] = "true"
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
        // Runtime Mixin support is supplied by these libs alongside it. The
        // common/modules jars carry the runtime module classes the mixins call
        // (EverlastingnessClient, WeatherChangerModule, ...) — without them the
        // handler bodies throw NoClassDefFoundError at apply time.
        ExtraClasspath =
        [
            "mixin-0.8.7.jar",
            "common-1.0.0-SNAPSHOT.jar",
            "modules-1.0.0-SNAPSHOT.jar",
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
        BuildAllVersions();

    /// <summary>
    /// Builds the complete version catalogue, mirroring Lunar Client's supported
    /// version matrix (verified 2026-08 against Lunar's launcher API). Each
    /// version is grouped into the injection era appropriate to its toolchain.
    /// </summary>
    private static Dictionary<string, EverlastingnessClientProfile> BuildAllVersions()
    {
        var all = new Dictionary<string, EverlastingnessClientProfile>();

        // --- Legacy era: LaunchWrapper + MCP/SRG mappings, MixinTweaker ---
        // 1.7.10 is the deepest legacy (full notch-obf at runtime) and the only
        // version with its own tweaker source set (v1_7_10). 1.8.9–1.12.2 jars
        // were rebuilt from the shared version-tolerant source (v1_20_x, agent
        // + StandaloneMixinService, obf-rewritten per-version), so they inject
        // through the modern -javaagent path on a modern JVM.
        all["1.7.10"] = Legacy("1.7.10", "everlastingness-1.7.10.jar");
        foreach (var v in new[] { "1.8.9", "1.9.4", "1.11.2", "1.12.2" })
            all[v] = Modern(v, $"everlastingness-{v}.jar");

        // --- Modern era: Java-agent injection, Mojang/Yarn mappings ---
        // Covers 1.16.5 through the newest 1.21.x and the 26.x rebrand. All use
        // the ModLauncher/agent injection path (no LaunchWrapper).
        string[] modern =
        {
            // 1.16–1.19
            "1.16.5", "1.17.1", "1.18.1", "1.18.2",
            "1.19", "1.19.2", "1.19.3", "1.19.4",
            // 1.20.x
            "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
            // 1.21.x (note: 1.21.2 is NOT supported by Lunar — private build)
            "1.21", "1.21.1", "1.21.3", "1.21.4", "1.21.5",
            "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
            // 26.x rebrand (Fabric-based)
            "26.1", "26.2"
        };
        foreach (var v in modern)
            all[v] = Modern(v, $"everlastingness-{v}.jar");

        return all;
    }

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

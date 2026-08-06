// Everlastingness client module for Minecraft 1.7.10.
//
// RetroFuturaGradle (RFG) is the only MC-1.7.10 Gradle toolchain in active
// maintenance: it sets up a deobfuscated (MCP) dev environment with Mixin
// support and produces a reobfuscated jar for the launcher to inject.
//
// RFG supports Gradle 7.6–8.8; the wrapper at the repo root pins 8.8.
// (Modern versions 1.16.5+ will live in a SEPARATE Gradle build because Fabric
// Loom 1.17 requires Gradle 9.x — the two cannot share a wrapper.)

plugins {
    id("java-library")
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.7"
}

// RFG requires a Java 8 toolchain to compile against 1.7.10.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

minecraft {
    mcVersion.set("1.7.10")
    username.set("Developer")

    // Mixin bootstraps via MixinTweaker on legacy LaunchWrapper; our own
    // ClientTweaker then wires the Everlastingness bootstrap on top.
    extraTweakClasses.add("org.spongepowered.asm.launch.MixinTweaker")
    extraTweakClasses.add("net.everlastingness.client.v1_7_10.tweaker.ClientTweaker")
}

// RFG injects its own repositories (MCP, GTNH) as project repositories, which
// shadow the dependencyResolutionManagement block. Declare Sponge Maven here so
// Mixin (org.spongepowered:mixin) resolves from this subproject too.
repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "Sponge Maven"
    }
    maven("https://nexus.gtnewhorizons.com/repository/public/") {
        name = "GTNH Maven"
    }
    mavenCentral()
}

dependencies {
    // Version-specific code depends on the shared common + modules libraries.
    implementation(project(":common"))
    implementation(project(":modules"))

    // SpongePowered Mixin runtime (LaunchWrapper-backed service variant).
    implementation("org.spongepowered:mixin:0.8.7")
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
}

// Configure the Mixin annotation processor: it must know the MCP→SRG mapping
// (shipped in the Forge 1.7.10 conf cache as mcp-srg.srg) so it can generate
// the refmap that remaps MCP-named @Inject/@Shadow targets
// (e.g. updateCameraAndRender → func_78480_b) for the reobfuscated jar.
//
// RFG downloads Forge 1.7.10 to the gradle cache during setupDecompWorkspace;
// the srgs/ subfolder holds the full mapping set. We locate the cache dir
// dynamically so the path is not hard-coded.
val forgeConfDir = file(System.getProperty("user.home") +
    "/.gradle/caches/minecraft/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/unpacked/srgs")
val mcpToSrg = file("$forgeConfDir/mcp-srg.srg")
tasks.withType<JavaCompile> {
    if (mcpToSrg.exists()) {
        options.compilerArgs.addAll(listOf(
            // Note the capital R in outRefMapFile — the processor rejects the
            // lowercase form. This emits mixins.everlastingness.refmap.json,
            // which the reobf jar bundles so Mixin can resolve MCP-named
            // @Inject targets against SRG names in production.
            "-AoutRefMapFile=${layout.buildDirectory.file("mixins.everlastingness.refmap.json").get().asFile.absolutePath}",
            "-AreobfSrgFile=${mcpToSrg.absolutePath}",
            "-AdefaultObfuscationEnv=searge"
        ))
    }
}

// Bundle the generated refmap AND the :common/:modules class outputs into the
// jar. RFG's reobfJar only processes :v1_7_10 source; the shared libraries
// (:common, :modules) must be included so the production jar is self-contained.
val refmapFile = layout.buildDirectory.file("mixins.everlastingness.refmap.json")
tasks.named<Jar>("jar") {
    dependsOn(":common:jar", ":modules:jar")
    from(refmapFile)
    // Include :common and :modules compiled class outputs.
    from(project(":common").layout.buildDirectory.dir("classes/java/main"))
    from(project(":modules").layout.buildDirectory.dir("classes/java/main"))
    // Include module service files.
    from(project(":modules").layout.buildDirectory.dir("resources/main"))
}

// The reobf task remaps MCP names back to SRG/Notch for the production jar.
// The launcher injects the resulting artifact as the per-version client jar.
tasks.named("reobfJar") {
    // RFG wires this automatically; left explicit for discoverability.
}

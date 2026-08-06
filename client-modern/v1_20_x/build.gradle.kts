// Everlastingness client module for Minecraft 1.20.x (modern era).
//
// Fabric Loom is used ONLY as a development toolchain: it gives us a
// deobfuscated (Yarn-named) dev environment + Mixin annotation processing.
// The production jar is injected at runtime via -javaagent and does NOT run
// under Fabric Loader — see the README "intermediary→official reobf gap" note.
//
// Fabric Maven no longer hosts older Loom releases, so we must use the current
// line: Loom 1.17.x, which requires Gradle 9.5+. On Loom 1.14+ the correct
// plugin id for OBFUSCATED MC versions (1.20.x) is `net.fabricmc.fabric-loom-remap`
// (the non-`-remap` variant targets the unobfuscated 1.21.11+ era).

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17.18"
    `maven-publish`
}

version = property("mod_version") as String
group = property("maven_group") as String

base {
    archivesName.set("everlastingness-1.20.1")
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "Sponge Maven"
    }
}

loom {
    mods {
        create("everlastingness") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    // The standalone Mixin host (StandaloneMixinService, MixinClassFileTransformer,
    // the agent premain) compiles directly against the Mixin + ASM API. Provided
    // at runtime by the mixin jar shaded into the agent (see processIncludeJars
    // below) — compileOnly here so it isn't duplicated in the output.
    compileOnly("org.spongepowered:mixin:0.8.7")
    compileOnly("org.ow2.asm:asm-tree:9.6")

    // Bundle Mixin + ASM into the production agent jar so the standalone host
    // works without Fabric Loader. Loom's processIncludeJars includes these.
    include("org.spongepowered:mixin:0.8.7")
    include("org.ow2.asm:asm-tree:9.6")
    include("org.ow2.asm:asm:9.6")
    include("org.ow2.asm:asm-commons:9.6")
    include("org.ow2.asm:asm-util:9.6")

    // NOTE: do NOT declare the Mixin annotation processor here. Loom wires it
    // automatically against the chosen mappings. Declaring it manually caused
    // the raw MCP AP to be used, which then could not resolve Yarn targets.
}

tasks.withType<JavaCompile>().configureEach {
    // MC 1.20.x runs on Java 17.
    options.release.set(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

// The standalone agent jar needs a Premain-Class manifest entry so
// `-javaagent:everlastingness-1.20.1.jar` invokes our premain. We add it to
// the remapped (production) jar.
tasks.named<Jar>("jar") {
    manifest {
        attributes(
            mapOf(
                "Premain-Class" to "net.everlastingness.client.agent.EverlastingnessAgent",
                "Agent-Class" to "net.everlastingness.client.agent.EverlastingnessAgent",
                "Can-Redefine-Classes" to "true",
                "Can-Retransform-Classes" to "true"
            )
        )
    }
}

// ============================================================================
// Build-time reobfuscation: intermediary → official (the reobf gap solution)
// ============================================================================
//
// Loom's remapJar emits an intermediary-namespaced jar. Vanilla runtime classes
// are official/obfuscated, so a standalone agent's mixins would silently fail.
// This task reobfuscates the remapJar output to official names using Tiny
// Remapper + MixinExtension (rewrites @Mixin/@Inject annotation strings too).
//
// The output (-official.jar) is what the launcher injects via -javaagent.
val reobfMappings = layout.projectDirectory.file("mappings/intermediary-1.20.1.tiny")

val reobfJar by tasks.registering(net.everlastingness.build.ReobfIntermediaryToOfficialTask::class) {
    group = "everlastingness"
    description = "Reobfuscate the Loom intermediary jar to official (obfuscated) names."

    // Input: the intermediary jar from Loom's remapJar.
    inputJar.set(tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar").flatMap { it.archiveFile })
    // Output: alongside the loom output, suffixed -official.
    outputJar.set(layout.buildDirectory.file("libs/${base.archivesName.get()}-${version}-official.jar"))
    mappingsFile.set(reobfMappings)
    // classpath is wired in afterEvaluate below (needs the resolved minecraft config).
}

// Tiny Remapper needs classpath roots to resolve @Inject/@Shadow targets: it
// can only rewrite an @Inject method string if it can enumerate the target
// class's members. The input mixin jar uses INTERMEDIARY names (class_757 /
// method_3192), so the classpath must include the intermediary-namespaced MC
// jar. The vanilla (official) jar is also added so Tiny Remapper can follow
// inheritance across both namespaces.
val loomCache = file("${System.getProperty("user.home")}/.gradle/caches/fabric-loom")
val vanillaClientJar = loomCache.resolve("${property("minecraft_version")}/minecraft-client.jar")
val mcMaven = loomCache.resolve("minecraftMaven/net/minecraft")
val intermediaryMcJar = mcMaven.resolve("minecraft-merged-intermediary")
    .listFiles()
    ?.firstOrNull { it.name.startsWith(property("minecraft_version") as String) }
    ?.let { it.resolve("minecraft-merged-intermediary-${it.name}.jar") }

// Make reobf depend on remapJar and feed it both MC jars as classpath roots.
afterEvaluate {
    val reobfTask = tasks.named<net.everlastingness.build.ReobfIntermediaryToOfficialTask>("reobfJar").get()
    reobfTask.dependsOn("remapJar")
    reobfTask.classpath.set(provider {
        buildList {
            if (intermediaryMcJar?.exists() == true) add(intermediaryMcJar.toPath())
            if (vanillaClientJar.exists()) add(vanillaClientJar.toPath())
        }
    })
}

tasks.named("build") {
    dependsOn("reobfJar")
}

// ============================================================================
// Produce the final standalone agent jar: reobf output + Mixin + ASM merged
// ============================================================================
//
// The agent runs under -javaagent on the system classloader, so Mixin + ASM
// (needed to bootstrap the standalone host) must be inside the single jar.
// Loom's `include` rejected the ASM semver, so we merge explicitly here.
val runtimeDepsForAgent = configurations.detachedConfiguration(
    dependencies.create("org.spongepowered:mixin:0.8.7"),
    dependencies.create("org.ow2.asm:asm-tree:9.6"),
    dependencies.create("org.ow2.asm:asm:9.6"),
    dependencies.create("org.ow2.asm:asm-commons:9.6"),
    dependencies.create("org.ow2.asm:asm-util:9.6")
)

val packageAgent by tasks.registering(net.everlastingness.build.MergeAgentDepsTask::class) {
    group = "everlastingness"
    description = "Merge the reobf'd client jar with Mixin + ASM into the standalone agent jar."

    baseJar.set(tasks.named<net.everlastingness.build.ReobfIntermediaryToOfficialTask>("reobfJar").flatMap { it.outputJar })
    outputJar.set(layout.buildDirectory.file(
        "libs/${base.archivesName.get()}-${version}-agent.jar"))
    dependencyJars.set(provider { runtimeDepsForAgent.files.toList().map { it.toPath() } })
    dependsOn("reobfJar")
}

tasks.named("build") {
    dependsOn("packageAgent")
}

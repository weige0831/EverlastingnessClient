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

    // NOTE: do NOT declare the Mixin annotation processor here. Loom wires it
    // automatically against the chosen mappings (and its own refmap handling).
    // Declaring it manually caused the raw MCP AP to be used, which then could
    // not resolve Yarn-named targets like `render`.
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

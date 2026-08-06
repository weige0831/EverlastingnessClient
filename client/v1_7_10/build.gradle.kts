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
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.0"
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

dependencies {
    // Version-specific code depends on the shared common + modules libraries.
    implementation(project(":common"))
    implementation(project(":modules"))

    // SpongePowered Mixin runtime (LaunchWrapper-backed service variant).
    implementation("org.spongepowered:mixin:0.8.7")
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
}

// The reobf task remaps MCP names back to SRG/Notch for the production jar.
// The launcher injects the resulting artifact as the per-version client jar.
tasks.named("reobfJar") {
    // RFG wires this automatically; left explicit for discoverability.
}

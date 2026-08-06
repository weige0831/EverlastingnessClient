rootProject.name = "EverlastingnessClient"

pluginManagement {
    repositories {
        // RetroFuturaGradle (1.7.10) is published here.
        maven("https://nexus.gtnewhorizons.com/repository/public/") {
            name = "GTNH Maven"
        }
        // SpongePowered Mixin and snapshots.
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "Sponge Maven"
        }
        gradlePluginPortal()
        mavenCentral()
    }
    // RFG is NOT on the Gradle Plugin Portal, so map the plugin id to its real
    // coordinate on GTNH Maven. Without this, Gradle looks for a plugin-marker
    // artifact that does not exist and fails to resolve.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.gtnewhorizons.retrofuturagradle") {
                useModule("com.gtnewhorizons:retrofuturagradle:${requested.version}")
            }
        }
    }
}

// RFG's decompile pipeline (Fernflower) requires a JDK 17+ toolchain. The
// foojay resolver lets Gradle auto-download the exact JDK when it isn't
// already installed locally.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "Sponge Maven"
        }
        maven("https://nexus.gtnewhorizons.com/repository/public/") {
            name = "GTNH Maven"
        }
        maven("https://maven.fabricmc.net/") {
            name = "Fabric Maven"
        }
    }
}

// Version-agnostic modules: shared library, injection agent, feature modules.
include(":common")
include(":agent")
include(":modules")

// Per-version client modules. Each applies the build toolchain appropriate
// for its era and produces a reobfuscated jar the launcher injects.
include(":v1_7_10")

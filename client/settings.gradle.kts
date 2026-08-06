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

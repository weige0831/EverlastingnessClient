pluginManagement {
    repositories {
        // Fabric Loom is published on the Fabric Maven, not the Gradle plugin
        // portal, so it must be resolvable here.
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
    }
    // The Loom plugin ids resolve to marker artifacts that Fabric Maven does
    // publish, so a resolutionStrategy is not needed for the 1.14+ ids.
}

rootProject.name = "EverlastingnessClientModern"

// Per-version modern subproject. Each targets a single obfuscated MC version
// (1.20.x here), compiled under Yarn mappings via Fabric Loom 1.17.x (Gradle
// 9.5+). The output is a standalone jar the launcher injects via -javaagent
// (NOT run under Fabric Loader — see README "reobf gap" note).
include(":v1_20_x")

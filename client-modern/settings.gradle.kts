pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "EverlastingnessClientModern"
include(":v1_20_x")

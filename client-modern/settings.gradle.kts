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
// Only one era module is configured per invocation: the yarn-based v1_20_x
// cannot resolve 26.x metadata (loader too old), and v26_x needs identity
// mappings. Select via -Pmodule= (default v1_20_x).
if (providers.gradleProperty("module").getOrElse("v1_20_x") == "v26_x") {
    include(":v26_x")
} else {
    include(":v1_20_x")
}

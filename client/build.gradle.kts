// Root build file. Per-version toolchain plugins (RetroFuturaGradle for 1.7.10,
// Fabric Loom for modern versions) are applied INSIDE each version subproject,
// never at the root — they fight over the `minecraft`/`loom` configuration
// namespace and cannot coexist on the root.

plugins {
    // Shared Java conventions for the version-agnostic modules.
    `java-library`
    `maven-publish`
}

// Global Java settings for the plain-Java modules (common, agent, modules).
// Per-version subprojects override these where their toolchain demands it.
allprojects {
    group = "net.everlastingness.client"
    version = "1.0.0-SNAPSHOT"
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            // Emit Java 8 bytecode so legacy Minecraft (1.7.10) can load the jars.
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        // Note: do NOT use options.release here — it requires a JDK 9+ compiler
        // and the project may build with JDK 8 (RFG) or 17/21 (Loom). The
        // source/targetCompatibility above already constrain the bytecode level.
    }
}

// The `common`, `agent`, and `modules` projects are plain Java; the
// version subprojects (v1_7_10, ...) apply RFG/Loom themselves.
project(":common") {
    apply(plugin = "java-library")
    dependencies {
        "api"("org.spongepowered:mixin:0.8.7")
    }
}

project(":agent") {
    apply(plugin = "java-library")
    dependencies {
        "implementation"(project(":common"))
        "implementation"("org.spongepowered:mixin:0.8.7")
    }
}

project(":modules") {
    apply(plugin = "java-library")
    dependencies {
        "implementation"(project(":common"))
    }
}

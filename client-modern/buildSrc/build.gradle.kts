// A tiny buildSrc project that provides the reobf task. Keeping it here lets the
// task class use the tiny-remapper API at configuration time without polluting
// the published client jar's dependencies.
plugins {
    `kotlin-dsl`
}
repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}
dependencies {
    // Tiny Remapper (library, not the Gradle plugin) + MixinExtension. The
    // MixinExtension rewrites @Mixin/@Inject/@At annotation strings alongside
    // bytecode references — without it the reobf would leave mixin targets in
    // intermediary names and they would silently fail to resolve at runtime.
    implementation("net.fabricmc:tiny-remapper:0.14.0")
    implementation("net.fabricmc:mapping-io:0.6.1")
}

// Everlastingness client module for Minecraft 1.7.10.
//
// RetroFuturaGradle (RFG) is the only MC-1.7.10 Gradle toolchain in active
// maintenance: it sets up a deobfuscated (MCP) dev environment with Mixin
// support and produces a reobfuscated jar for the launcher to inject.

import java.io.File
//
// RFG supports Gradle 7.6–8.8; the wrapper at the repo root pins 8.8.
// (Modern versions 1.16.5+ will live in a SEPARATE Gradle build because Fabric
// Loom 1.17 requires Gradle 9.x — the two cannot share a wrapper.)

plugins {
    id("java-library")
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.7"
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

// RFG injects its own repositories (MCP, GTNH) as project repositories, which
// shadow the dependencyResolutionManagement block. Declare Sponge Maven here so
// Mixin (org.spongepowered:mixin) resolves from this subproject too.
repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "Sponge Maven"
    }
    maven("https://nexus.gtnewhorizons.com/repository/public/") {
        name = "GTNH Maven"
    }
    mavenCentral()
}

dependencies {
    // Version-specific code depends on the shared common + modules libraries.
    implementation(project(":common"))
    implementation(project(":modules"))

    // SpongePowered Mixin runtime (LaunchWrapper-backed service variant).
    implementation("org.spongepowered:mixin:0.8.7")
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
}

// Configure the Mixin annotation processor: it must know the MCP→SRG mapping
// (shipped in the Forge 1.7.10 conf cache as mcp-srg.srg) so it can generate
// the refmap that remaps MCP-named @Inject/@Shadow targets
// (e.g. updateCameraAndRender → func_78480_b) for the reobfuscated jar.
//
// RFG downloads Forge 1.7.10 to the gradle cache during setupDecompWorkspace;
// the srgs/ subfolder holds the full mapping set. We locate the cache dir
// dynamically so the path is not hard-coded.
val forgeConfDir = file(System.getProperty("user.home") +
    "/.gradle/caches/minecraft/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/unpacked/srgs")
val mcpToSrg = file("$forgeConfDir/mcp-srg.srg")
tasks.withType<JavaCompile> {
    if (mcpToSrg.exists()) {
        options.compilerArgs.addAll(listOf(
            // Note the capital R in outRefMapFile — the processor rejects the
            // lowercase form. This emits mixins.everlastingness.refmap.json,
            // which the reobf jar bundles so Mixin can resolve MCP-named
            // @Inject targets against SRG names in production.
            "-AoutRefMapFile=${layout.buildDirectory.file("mixins.everlastingness.refmap.json").get().asFile.absolutePath}",
            "-AreobfSrgFile=${mcpToSrg.absolutePath}",
            "-AdefaultObfuscationEnv=searge"
        ))
    }
}

// Bundle the generated refmap AND the :common/:modules class outputs into the
// jar. RFG's reobfJar only processes :v1_7_10 source; the shared libraries
// (:common, :modules) must be included so the production jar is self-contained.
val refmapFile = layout.buildDirectory.file("mixins.everlastingness.refmap.json")

// Post-process the refmap to add a "notch" data section. MC 1.7.10 ships with
// NOTCH-obfuscated class names (e.g. blt) but SRG method names (func_*). The
// annotation processor's reobfSrgFile (mcp-srg.srg) only emits method remaps
// and leaves class names in MCP/SRG form — which the runtime MC jar cannot
// resolve (its classes are notch-named). We build a notch section that rewrites
// the owner class to its notch name using notch-srg.srg (notch→SRG class),
// inverted. At runtime we set mixin.env.refMapRemappingEnv=notch so Mixin picks
// this section and resolves Lblt;func_* correctly.
val notchSrg = file("$forgeConfDir/notch-srg.srg")
val patchRefmap = tasks.register("patchRefmapForNotch") {
    dependsOn("compileJava") // refmap emitted by the annotation processor during compile
    val refmapPath = refmapFile.get().asFile
    val notchSrgPath = notchSrg
    // Do NOT declare inputs.file for refmapPath (it doesn't exist at config
    // time); declare the action inputs lazily instead.
    doLast {
        if (!refmapPath.exists() || !notchSrgPath.exists()) {
            logger.warn("patchRefmapForNotch: refmap or notch-srg.srg missing, skipping")
            return@doLast
        }
        // Build SRG-class → notch-class map from notch-srg.srg (CL: <notch> <srg>).
        val srgToNotch = HashMap<String, String>()
        notchSrgPath.useLines { lines ->
            for (line in lines) {
                if (line.startsWith("CL: ")) {
                    val parts = line.split(" ")
                    if (parts.size >= 3) {
                        // parts[1] = notch, parts[2] = srg class name
                        srgToNotch[parts[2]] = parts[1]
                    }
                }
            }
        }
        logger.lifecycle("patchRefmapForNotch: loaded ${srgToNotch.size} notch class mappings")

        // Read the existing refmap JSON and add a "notch" data section by
        // rewriting the owner class in each "searge" entry.
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        val json = gson.fromJson(refmapPath.readText(), Map::class.java)
        @Suppress("UNCHECKED_CAST")
        val data = json["data"] as MutableMap<String, MutableMap<String, MutableMap<String, String>>>
        val searge = data["searge"]
        if (searge != null) {
            val notch = LinkedHashMap<String, MutableMap<String, String>>()
            for ((mixinName, methodMap) in searge) {
                val remapped = LinkedHashMap<String, String>()
                for ((methodName, ref) in methodMap) {
                    // ref looks like "Lnet/minecraft/.../EntityRenderer;func_xxx(...)V"
                    // Rewrite the owner (between L and ;) to its notch name.
                    remapped[methodName] = rewriteOwner(ref, srgToNotch)
                }
                notch[mixinName] = remapped
            }
            data["notch"] = notch
        }
        // Rebuild the top-level "mappings" too (used as fallback). Leave as-is.

        refmapPath.writeText(gson.toJson(json))
        logger.lifecycle("patchRefmapForNotch: added 'notch' section to ${refmapPath.name}")
    }
}

fun rewriteOwner(ref: String, srgToNotch: Map<String, String>): String {
    // ref format: L<owner>;<method>(<desc>)  OR  plain text
    val semi = ref.indexOf(';')
    if (semi < 0 || !ref.startsWith("L")) return ref
    val owner = ref.substring(1, semi)
    val notch = srgToNotch[owner] ?: owner
    return "L" + notch + ref.substring(semi)
}

tasks.named<Jar>("jar") {
    dependsOn(":common:jar", ":modules:jar", patchRefmap)
    from(refmapFile)
    // Include :common and :modules compiled class outputs.
    from(project(":common").layout.buildDirectory.dir("classes/java/main"))
    from(project(":modules").layout.buildDirectory.dir("classes/java/main"))
    // Include module service files.
    from(project(":modules").layout.buildDirectory.dir("resources/main"))
}

// The reobf task remaps MCP names back to SRG/Notch for the production jar.
// The launcher injects the resulting artifact as the per-version client jar.
tasks.named("reobfJar") {
    // RFG wires this automatically; left explicit for discoverability.
    dependsOn(patchRefmap)
    finalizedBy("patchMixinTargets")
}

// Post-reobf task: rewrite the @Mixin(value=X.class) annotations in the
// reobfuscated jar so that the target class names are the notch-obfuscated
// runtime names (blt, bao, ...). MC 1.7.10 ships with notch class names, so
// Mixin's target lookup for "net.minecraft.client.renderer.EntityRenderer"
// fails; rewriting to targets={"blt"} makes it resolve at runtime.
val patchMixinTargets = tasks.register("patchMixinTargets") {
    dependsOn("reobfJar")
    val reobfJar = layout.buildDirectory.file("libs/v1_7_10-1.0.0-SNAPSHOT.jar")
    val notchSrgPath = notchSrg
    val patcherSrc = layout.projectDirectory.file("build-tools/src/MixinTargetPatcher.java").asFile
    val patcherOut = layout.buildDirectory.dir("tools-classes").get().asFile
    inputs.file(reobfJar)
    inputs.file(notchSrgPath)
    inputs.file(patcherSrc)
    outputs.file(reobfJar)
    doLast {
        val jarFile = reobfJar.get().asFile
        if (!jarFile.exists()) {
            logger.warn("patchMixinTargets: reobf jar not found at $jarFile, skipping")
            return@doLast
        }
        // Compile the patcher against asm 9.6.
        val asmDeps = configurations.detachedConfiguration(
            dependencies.create("org.ow2.asm:asm:9.6"),
            dependencies.create("org.ow2.asm:asm-tree:9.6"),
            dependencies.create("org.ow2.asm:asm-commons:9.6")
        ).files
        patcherOut.mkdirs()
        val cp = asmDeps.joinToString(File.pathSeparator)
        // Gradle daemon runs on the JDK pointed by JAVA_HOME; System.getProperty
        // ("java.home") on JDK 8 returns the nested jre/ dir whose bin/ has no
        // javac, so prefer JAVA_HOME explicitly.
        val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
        val javacExe = javaHome + File.separator + "bin" + File.separator + "javac"
        val javaExe = javaHome + File.separator + "bin" + File.separator + "java"
        val compileArgs = ArrayList<String>()
        compileArgs.add(javacExe)
        compileArgs.add("-encoding"); compileArgs.add("UTF-8")
        compileArgs.add("-d"); compileArgs.add(patcherOut.absolutePath)
        compileArgs.add("-cp"); compileArgs.add(cp)
        compileArgs.add(patcherSrc.absolutePath)
        val compileProc = ProcessBuilder(compileArgs).redirectErrorStream(true).start()
        val compileOut = compileProc.inputStream.readBytes().toString(Charsets.UTF_8)
        val compileCode = compileProc.waitFor()
        if (compileCode != 0) {
            throw GradleException("patchMixinTargets: javac failed ($compileCode):\n$compileOut")
        }
        // Run the patcher.
        val runArgs = ArrayList<String>()
        runArgs.add(javaExe)
        runArgs.add("-cp"); runArgs.add(patcherOut.absolutePath + File.pathSeparator + cp)
        runArgs.add("MixinTargetPatcher")
        runArgs.add(jarFile.absolutePath)
        runArgs.add(notchSrgPath.absolutePath)
        val runProc = ProcessBuilder(runArgs).redirectErrorStream(true).start()
        val runOut = runProc.inputStream.readBytes().toString(Charsets.UTF_8)
        val runCode = runProc.waitFor()
        logger.lifecycle(runOut.trim())
        if (runCode != 0) {
            throw GradleException("patchMixinTargets: patcher failed ($runCode)")
        }
    }
}


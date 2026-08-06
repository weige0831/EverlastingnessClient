package net.everlastingness.client.v1_7_10.tweaker;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unified tweak class that replaces both the separate PreTweaker and
 * MixinTweaker. The problem with using SpongePowered's MixinTweaker directly
 * is that its static &lt;clinit&gt; triggers MixinService.boot() via ServiceLoader
 * BEFORE any ITweaker.injectIntoClassLoader() can add the necessary classloader
 * exclusions — because Launch.launch() instantiates all tweak classes before
 * calling injectIntoClassLoader on any of them.
 *
 * <p>This class avoids that by NOT using MixinTweaker at all. Instead it:</p>
 * <ol>
 *   <li>Adds <code>org.spongepowered.asm.</code> to LaunchClassLoader's
 *       classloader exclusions (so Mixin service classes are loaded by the
 *       parent/system classloader, not transformed by LaunchClassLoader).</li>
 *   <li>Manually triggers MixinBootstrap.init() — at this point the
 *       exclusions are in place, so MixinService.getService() will find
 *       MixinServiceLaunchWrapper and its isValid() will pass.</li>
 *   <li>Registers the mixin config via Mixins.addConfiguration().</li>
 *   <li>Registers a Proxy transformer on LaunchClassLoader so Mixin's
 *       IMixinTransformer is called for every class load.</li>
 * </ol>
 */
public class EverlastingnessPreTweaker implements ITweaker {

    private static boolean initialised = false;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (initialised) return;
        initialised = true;

        System.out.println("[Everlastingness] EverlastingnessPreTweaker: initialising Mixin host");

        // Debug: check Launch.classLoader state
        try {
            Class<?> launchCls = Class.forName("net.minecraft.launchwrapper.Launch");
            Object cl = launchCls.getField("classLoader").get(null);
            System.out.println("[Everlastingness] Launch.classLoader = " + cl);
        } catch (Throwable t) {
            System.out.println("[Everlastingness] Launch.classLoader check failed: " + t);
        }

        // Debug: print java.class.path and check if launchwrapper classes are loadable
        System.out.println("[Everlastingness] java.class.path = " + System.getProperty("java.class.path", "(not set)"));
        try {
            Class.forName("net.minecraft.launchwrapper.LaunchClassLoader");
            System.out.println("[Everlastingness] LaunchClassLoader loadable from system cl: YES");
        } catch (Throwable t) {
            System.out.println("[Everlastingness] LaunchClassLoader loadable from system cl: NO - " + t.getMessage());
        }

        // 1. Add classloader exclusions so Mixin's own classes are loaded by
        //    the system classloader, not transformed by LaunchClassLoader.
        //    the system classloader, not transformed by LaunchClassLoader.
        //    This is what makes ServiceLoader discover MixinServiceLaunchWrapper.
        //    Do NOT exclude net.everlastingness.* — our client classes must
        //    all load through LaunchClassLoader together.
        classLoader.addClassLoaderExclusion("org.spongepowered.asm.");
        System.out.println("[Everlastingness] classloader exclusions added");

        // 2. Manually bootstrap Mixin — NOT via MixinTweaker (which has the
        //    static-init-before-exclusions problem), but by calling
        //    MixinBootstrap directly. Use the thread context classloader
        //    (LaunchClassLoader), which now has the asm exclusion set.
        try {
            // Set context classloader to system so ServiceLoader finds
            // services from the system classpath.
            Thread currentThread = Thread.currentThread();
            ClassLoader originalCl = currentThread.getContextClassLoader();
            ClassLoader sysCl = ClassLoader.getSystemClassLoader();
            currentThread.setContextClassLoader(sysCl);

            Class<?> bootstrapCls = Class.forName(
                "org.spongepowered.asm.launch.MixinBootstrap", true, sysCl);
            java.lang.reflect.Method init = bootstrapCls.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(null);

            // Restore original context classloader.
            currentThread.setContextClassLoader(originalCl);
            System.out.println("[Everlastingness] MixinBootstrap.init() succeeded");
        } catch (Throwable t) {
            System.err.println("[Everlastingness] MixinBootstrap.init() failed: " + t);
            t.printStackTrace();
        }

        // 3. Register mixin configs.
        try {
            ClassLoader sysCl = ClassLoader.getSystemClassLoader();
            Class<?> mixinsCls = Class.forName(
                "org.spongepowered.asm.mixin.Mixins", true, sysCl);
            java.lang.reflect.Method addConfig = mixinsCls.getDeclaredMethod(
                "addConfiguration", String.class);
            String configs = System.getProperty("mixin.configs", "mixins.everlastingness.json");
            for (String cfg : configs.split(",")) {
                String trimmed = cfg.trim();
                if (!trimmed.isEmpty()) {
                    addConfig.invoke(null, trimmed);
                    System.out.println("[Everlastingness] mixin config registered: " + trimmed);
                }
            }
        } catch (Throwable t) {
            System.err.println("[Everlastingness] mixin config registration failed: " + t);
            t.printStackTrace();
        }

        // 4. Register Mixin's transformer as a LaunchClassLoader transformer.
        try {
            ClassLoader sysCl = ClassLoader.getSystemClassLoader();
            Class<?> envCls = Class.forName(
                "org.spongepowered.asm.mixin.MixinEnvironment", true, sysCl);
            Object env = envCls.getMethod("getDefaultEnvironment").invoke(null);
            Object transformer = envCls.getMethod("getActiveTransformer").invoke(env);

            if (transformer != null) {
                // Register as a class transformer on LaunchClassLoader
                classLoader.registerTransformer(
                    "org.spongepowered.asm.mixin.transformer.Proxy");
                System.out.println("[Everlastingness] Mixin Proxy transformer registered");
            } else {
                System.out.println("[Everlastingness] WARNING: active transformer is null");
            }
        } catch (Throwable t) {
            System.err.println("[Everlastingness] transformer registration: " + t);
            // Non-fatal: some launchwrapper versions don't support registerTransformer
            // by class name. The Mixin Proxy class handles this internally.
        }

        System.out.println("[Everlastingness] EverlastingnessPreTweaker complete");
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.Minecraft";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}

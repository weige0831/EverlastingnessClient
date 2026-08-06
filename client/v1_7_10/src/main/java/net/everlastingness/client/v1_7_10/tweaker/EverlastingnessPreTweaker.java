package net.everlastingness.client.v1_7_10.tweaker;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.List;

/**
 * Pre-tweak class that adds the Mixin package to LaunchClassLoader's exclusion
 * list BEFORE MixinTweaker loads. Without this exclusion, Mixin's static
 * initializer triggers ServiceLoader discovery using LaunchClassLoader, which
 * transforms Mixin's own classes and breaks service discovery.
 *
 * <p>Launch order: PreTweaker → MixinTweaker → ClientTweaker.
 * All tweakers are instantiated first, then injectIntoClassLoader is called
 * in order. But since MixinTweaker's static init fires during instantiation,
 * we can't use injectIntoClassLoader. Instead we add the exclusion in the
 * CONSTRUCTOR of this class — which runs during Launch.launch()'s tweak class
 * loading loop, BEFORE MixinTweaker is loaded.</p>
 */
public class EverlastingnessPreTweaker implements ITweaker {

    static {
        // This static block runs when Launch loads this class via
        // Class.forName — BEFORE it loads MixinTweaker (since PreTweaker
        // is listed first in --tweakClass). We add the exclusion here.
        try {
            LaunchClassLoader cl = net.minecraft.launchwrapper.Launch.classLoader;
            if (cl != null) {
                cl.addClassLoaderExclusion("org.spongepowered.asm.");
                System.out.println("[Everlastingness] PreTweaker static: org.spongepowered.asm. exclusion added");
            } else {
                System.out.println("[Everlastingness] PreTweaker static: Launch.classLoader is null!");
            }
        } catch (Throwable t) {
            System.err.println("[Everlastingness] PreTweaker static init failed: " + t);
        }
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        System.out.println("[Everlastingness] PreTweaker.injectIntoClassLoader: no-op (exclusion already added in static init)");
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}

package net.everlastingness.client.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;

import net.everlastingness.client.mixinhost.MixinClassFileTransformer;

/**
 * Java Agent premain for the modern-era (1.20.x) Everlastingness client.
 *
 * <p>The launcher starts vanilla 1.20.x with
 * {@code -javaagent:everlastingness-1.20.1.jar}; this {@code premain} runs
 * before {@code net.minecraft.client.main.Main} and bootstraps the standalone
 * Mixin host so the reobfuscated mixin jar's hooks (e.g. {@code @Mixin(fjq)}
 * on the obfuscated {@code GameRenderer}) apply to the vanilla classes.</p>
 *
 * <p>Bootstrap sequence (the standalone Mixin host, no LaunchWrapper):</p>
 * <ol>
 *   <li>{@link MixinBootstrap#init()} triggers service discovery via
 *       {@code ServiceLoader}, selecting our {@code StandaloneMixinService}
 *       (its {@code isValid()} returns true) and offering the transformer
 *       factory to it.</li>
 *   <li>Retrieve the {@link IMixinTransformerFactory} internal from the service
 *       and build an {@link IMixinTransformer}.</li>
 *   <li>Load the mixin config ({@code everlastingness.mixins.json}) so the
 *       transformer knows which mixins to apply.</li>
 *   <li>Register a {@link MixinClassFileTransformer} wrapping the transformer
 *       with the JVM {@link Instrumentation}, so every class load is routed
 *       through Mixin.</li>
 * </ol>
 */
public class EverlastingnessAgent {

    private static final Logger LOGGER = Logger.getLogger("Everlastingness/Agent");

    public static void premain(String args, Instrumentation instrumentation) {
        String mcVersion = System.getProperty("everlastingness.version", "1.20.x");
        LOGGER.info("Everlastingness agent attached (premain) for Minecraft " + mcVersion);

        if (instrumentation == null) {
            LOGGER.severe("No Instrumentation available — cannot apply mixins. Aborting.");
            return;
        }

        try {
            bootstrapMixin(instrumentation, mcVersion);
        } catch (Throwable t) {
            LOGGER.severe("Failed to bootstrap Mixin host: " + t);
            t.printStackTrace();
        }
    }

    /** Agentmain entry for dynamic attach (same body as premain). */
    public static void agentmain(String args, Instrumentation instrumentation) {
        premain(args, instrumentation);
    }

    private static void bootstrapMixin(Instrumentation instrumentation, String mcVersion) throws Exception {
        // 1. Bootstrap Mixin: discover our StandaloneMixinService via ServiceLoader,
        //    prepare the environment, and offer the transformer factory to it.
        LOGGER.info("Bootstrapping Mixin host...");
        Class<?> bootstrap = Class.forName("org.spongepowered.asm.launch.MixinBootstrap");
        Method init = bootstrap.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(null);

        // 2. Obtain the transformer via the IMixinTransformerFactory internal that
        //    MixinBootstrap offered to the service during init().
        IMixinService service = MixinService.getService();
        IMixinTransformerFactory factory = getFactory(service);
        if (factory == null) {
            LOGGER.severe("Mixin service did not offer an IMixinTransformerFactory. " +
                    "Mixins will not apply.");
            return;
        }
        IMixinTransformer transformer = factory.createTransformer();
        LOGGER.info("Mixin transformer acquired.");

        // 3. Load the mixin configuration(s). The config is bundled in the agent
        //    jar (src/main/resources/everlastingness.mixins.json).
        String configs = System.getProperty("mixin.configs", "everlastingness.mixins.json");
        for (String config : configs.split(",")) {
            String trimmed = config.trim();
            if (!trimmed.isEmpty()) {
                Mixins.addConfiguration(trimmed);
                LOGGER.info("Loaded mixin config: " + trimmed);
            }
        }

        // 4. Register the transformer so every class load is routed through Mixin.
        instrumentation.addTransformer(new MixinClassFileTransformer(transformer), true);
        LOGGER.info("Mixin ClassFileTransformer registered. Everlastingness client active on "
                + mcVersion + ".");
    }

    /**
     * Retrieve the {@link IMixinTransformerFactory} that {@code MixinBootstrap}
     * offered to the service. {@code getInternal(Class)} is protected on
     * {@code MixinServiceAbstract}; reach it via reflection.
     */
    private static IMixinTransformerFactory getFactory(IMixinService service) throws Exception {
        if (service instanceof IMixinTransformerFactory) {
            return (IMixinTransformerFactory) service;
        }
        Method getInternal = service.getClass().getMethod("getInternal", Class.class);
        getInternal.setAccessible(true);
        return (IMixinTransformerFactory) getInternal.invoke(service, IMixinTransformerFactory.class);
    }
}

package net.everlastingness.client.mixinhost;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

/**
 * Standalone SpongePowered Mixin service for a Java-agent-hosted client on
 * modern Minecraft (1.20.x, Java 17) <strong>without</strong> LaunchWrapper,
 * Fabric Loader, or ModLauncher.
 *
 * <p>Extends {@link MixinServiceAbstract} (which lives in Mixin's main source
 * set and has no LaunchWrapper dependency), implementing only the abstract
 * methods. The required {@link IClassProvider} / {@link IClassBytecodeProvider}
 * are self-contained POJOs that delegate to {@link Class} / the system
 * classloader — the same pattern Cleanroom's {@code CleanMixService} uses.</p>
 *
 * <p>Discovered by Mixin's {@code MixinService.getService()} via the
 * {@code META-INF/services/org.spongepowered.asm.service.IMixinService} file.
 * {@link #isValid()} returns {@code true} unconditionally so this service is
 * selected over any other (e.g. the launchwrapper one which is not present).</p>
 */
public final class StandaloneMixinService extends MixinServiceAbstract implements IMixinTransformerFactory {

    private final IClassProvider classProvider = new StandaloneClassProvider();
    private final IClassBytecodeProvider bytecodeProvider = new StandaloneBytecodeProvider();
    private final IClassTracker classTracker = new StandaloneClassTracker();

    @Override
    public IMixinTransformer createTransformer() throws MixinInitialisationError {
        // MixinTransformer is package-private; instantiate via reflection.
        try {
            Class<?> clazz = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");
            java.lang.reflect.Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (IMixinTransformer) ctor.newInstance();
        } catch (Throwable t) {
            throw new MixinInitialisationError("Could not create MixinTransformer: " + t);
        }
    }


    @Override
    public org.spongepowered.asm.service.IFeatureValidator getFeatureValidator() {
        return org.spongepowered.asm.service.IFeatureValidator.ALLOW_ALL;
    }

    @Override
    public org.spongepowered.asm.service.IAdviceProvider getAdviceProvider() {
        // 26.x MC classes are Java 25 bytecode; advise Mixin to accept them.
        return new org.spongepowered.asm.service.IAdviceProvider() {
            @Override
            public String higherCompatibilityNeeded(int major, String owner) {
                return null; // no extra advice needed; ASM 9.8 handles v69
            }
        };
    }

    @Override
    public String getName() {
        return "EverlastingnessStandalone";
    }

    @Override
    public boolean isValid() {
        // Always valid — we are the host in a standalone agent environment.
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return classProvider;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return bytecodeProvider;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        // Optional; null is permitted by the contract.
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return classTracker;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        // Optional; null is permitted by the contract.
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        // The ClientPlatformAgent reports the side as CLIENT so the config's
        // "client" mixin list is selected (with no agents the side resolves to
        // UNKNOWN and no client-listed mixin is ever applied).
        return Collections.singletonList(
                "net.everlastingness.client.mixinhost.ClientPlatformAgent");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return new ContainerHandleVirtual(getName());
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        // MC 1.20.x runs on Java 17.
        return MixinEnvironment.CompatibilityLevel.JAVA_17;
    }

    @Override
    protected org.spongepowered.asm.logging.ILogger createLogger(String name) {
        // The default adapter discards ALL Mixin log output — including the
        // errors explaining why a mixin was rejected. Route to stderr.
        return new org.spongepowered.asm.logging.LoggerAdapterConsole(name);
    }
}

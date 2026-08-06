package net.everlastingness.client.mixinhost;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
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
public final class StandaloneMixinService extends MixinServiceAbstract {

    private final IClassProvider classProvider = new StandaloneClassProvider();
    private final IClassBytecodeProvider bytecodeProvider = new StandaloneBytecodeProvider();
    private final IClassTracker classTracker = new StandaloneClassTracker();

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
        // No platform agents (no Fabric/Forge integration).
        return Collections.emptyList();
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
}

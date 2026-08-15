package net.everlastingness.client.mixinhost;

import java.util.Collection;
import java.util.Collections;

import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.IConsumer;

/**
 * Tells Mixin which side the host process is. Without a platform agent
 * {@code MixinServiceAbstract.getSideName()} resolves to UNKNOWN and the
 * {@code "client"} mixin list in the config is silently ignored.
 */
public final class ClientPlatformAgent implements IMixinPlatformServiceAgent {

    @Override
    public void init() {
    }

    @Override
    public String getSideName() {
        return "CLIENT";
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return Collections.emptyList();
    }

    @Override
    public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
    }

    @Override
    public void unwire() {
    }

    @Override
    public AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
        return IMixinPlatformAgent.AcceptResult.REJECTED;
    }

    @Override
    public String getPhaseProvider() {
        return null;
    }

    @Override
    public void prepare() {
    }

    @Override
    public void initPrimaryContainer() {
    }

    @Override
    public void inject() {
    }
}

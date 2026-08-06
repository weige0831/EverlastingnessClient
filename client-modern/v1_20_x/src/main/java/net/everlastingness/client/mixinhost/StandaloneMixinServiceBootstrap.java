package net.everlastingness.client.mixinhost;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

/**
 * Standalone Mixin service bootstrap, discovered by Mixin's core via the Java
 * {@link java.util.ServiceLoader} file
 * {@code META-INF/services/org.spongepowered.asm.service.IMixinServiceBootstrap}.
 *
 * <p>This is the entry point Mixin's {@code MixinService.boot()} calls during
 * discovery. Its only real job is to name the {@link IMixinService} class that
 * pairs with it; the service itself does the work.</p>
 */
public final class StandaloneMixinServiceBootstrap implements IMixinServiceBootstrap {

    @Override
    public String getName() {
        return "EverlastingnessStandalone";
    }

    @Override
    public String getServiceClassName() {
        // MUST match the FQCN registered in
        // META-INF/services/org.spongepowered.asm.service.IMixinService
        return StandaloneMixinService.class.getName();
    }

    @Override
    public void bootstrap() {
        // No-op: the real service initialisation happens in StandaloneMixinService.
    }
}

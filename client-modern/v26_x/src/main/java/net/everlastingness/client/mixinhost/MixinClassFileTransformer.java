package net.everlastingness.client.mixinhost;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

/**
 * A {@link ClassFileTransformer} backed by Mixin's {@link IMixinTransformer}.
 *
 * <p>Registered with the JVM {@link java.lang.instrument.Instrumentation} in the
 * agent premain, this forwards every class-load event to Mixin's
 * {@code transformClassBytes}, which applies any configured mixin whose target
 * matches the class being loaded.</p>
 */
public final class MixinClassFileTransformer implements ClassFileTransformer {

    private final IMixinTransformer transformer;

    public MixinClassFileTransformer(IMixinTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public byte[] transform(ClassLoader loader, String internalName, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (internalName == null) {
            return null;
        }
        // Mixin expects the dotted binary name.
        String name = internalName.replace('/', '.');
        try {
            calls++;
            if (calls == 1 || calls == 1000 || calls == 10000) {
                System.out.println("[Everlastingness] transform call #" + calls + " for " + name);
            }
            byte[] result = transformer.transformClassBytes(name, name, classfileBuffer);
            if (result != classfileBuffer) {
                transforms++;
                if (transforms <= 5) {
                    System.out.println("[Everlastingness] transformed " + name + " (total " + transforms + ")");
                }
            }
            return result;
        } catch (Throwable t) {
            // Never let a Mixin error prevent the class from loading.
            System.err.println("[Everlastingness] Mixin transform failed for " + name + ": " + t);
            t.printStackTrace();
            return null;
        }
    }

    private int transforms;
    private int calls;
}

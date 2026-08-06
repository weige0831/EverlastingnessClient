package net.everlastingness.client.mixinhost;

import java.net.URL;
import java.util.Collections;

import org.spongepowered.asm.service.IClassProvider;

/**
 * Minimal {@link IClassProvider} that resolves classes through the system
 * classloader. This satisfies Mixin's requirement to look up target classes by
 * name during mixin application.
 */
final class StandaloneClassProvider implements IClassProvider {

    @Override
    @SuppressWarnings("deprecation")
    public URL[] getClassPath() {
        // Deprecated and unused by modern Mixin internals; empty is acceptable.
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        // Agent classes live in the system classloader in our setup.
        return Class.forName(name, initialize, ClassLoader.getSystemClassLoader());
    }
}

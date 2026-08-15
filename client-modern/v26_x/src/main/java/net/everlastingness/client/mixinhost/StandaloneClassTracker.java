package net.everlastingness.client.mixinhost;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.service.IClassTracker;

/**
 * Minimal {@link IClassTracker}: tracks classes Mixin marks invalid (so failed
 * loads aren't retried) and reports loaded/restricted state via the system
 * classloader.
 */
final class StandaloneClassTracker implements IClassTracker {

    private final Set<String> invalidClasses = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void registerInvalidClass(String className) {
        invalidClasses.add(className);
    }

    @Override
    public boolean isClassLoaded(String className) {
        // In a Java-agent environment the system classloader is the one loading
        // game classes; query it via the public reflection API.
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod(
                    "findLoadedClass", String.class);
            m.setAccessible(true);
            for (ClassLoader c = cl; c != null; c = c.getParent()) {
                if (m.invoke(c, className) != null) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Reflection failure: fall back to the conservative answer.
        }
        return false;
    }

    @Override
    public String getClassRestrictions(String className) {
        // No per-class restrictions in a standalone agent.
        return "";
    }
}

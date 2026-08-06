package net.everlastingness.client.mixinhost;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

/**
 * Standalone {@link IGlobalPropertyService} for the Everlastingness Mixin host.
 *
 * <p>Mixin's boot path requires a global-property ("blackboard") service in
 * addition to {@link StandaloneMixinService}. Mixin ships two implementations
 * ({@code mojang.Blackboard}, {@code modlauncher.Blackboard}) but both require
 * LaunchWrapper / ModLauncher hosts and report {@code isValid() == false} in our
 * standalone environment — leaving {@code MixinBootstrap.init()} to fail with
 * {@code ServiceNotAvailableError: No mixin global property service is available}.</p>
 *
 * <p>This implementation is a trivial in-memory key/value map: keys are the
 * property name strings wrapped in {@link Key}, values stored in a
 * {@link HashMap}. It is host-agnostic and always valid, satisfying the boot
 * requirement. Discovered via {@code ServiceLoader} (registered in
 * {@code META-INF/services/org.spongepowered.asm.service.IGlobalPropertyService}).</p>
 */
public final class StandaloneGlobalPropertyService implements IGlobalPropertyService {

    private final Map<String, Object> properties = new HashMap<>();

    /** Simple key wrapping a property name. */
    public static final class Key implements IPropertyKey {
        private final String name;
        public Key(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    @Override
    public IPropertyKey resolveKey(String name) {
        return new Key(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key) {
        return (T) properties.get(key.toString());
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        properties.put(key.toString(), value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        Object value = properties.get(key.toString());
        return value == null ? defaultValue : (T) value;
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = properties.get(key.toString());
        return value == null ? defaultValue : String.valueOf(value);
    }
}

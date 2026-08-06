package net.everlastingness.client.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.everlastingness.client.common.module.AbstractModule;
import net.everlastingness.client.common.module.Module;

/**
 * Unit tests for the module framework: registration, enable/disable lifecycle,
 * config-backed flags, and lookup. Exercises the same {@link EverlastingnessClient}
 * singleton the runtime uses, but with stub modules (no Mixin / MC).
 */
class ModuleFrameworkTest {

    /** A minimal stub module that counts enable/disable transitions. */
    static final class StubModule extends AbstractModule {
        final AtomicInteger enables = new AtomicInteger();
        final AtomicInteger disables = new AtomicInteger();
        final String id;
        StubModule(String id) { this.id = id; }
        @Override public String getId() { return id; }
        @Override public String getName() { return "Stub " + id; }
        @Override public void onEnable() { enables.incrementAndGet(); }
        @Override public void onDisable() { disables.incrementAndGet(); }
    }

    @BeforeAll
    static void initSingleton() {
        // Idempotent: returns the existing instance if already initialised.
        EverlastingnessClient.init("test");
    }

    @Test
    void registerEnablesByDefault() {
        EverlastingnessClient client = EverlastingnessClient.get();
        assertNotNull(client);
        StubModule m = new StubModule("stub_default_" + System.nanoTime());
        client.registerModule(m);

        assertTrue(m.isEnabled(), "a module with default-enabled should be enabled after register");
        assertEquals(1, m.enables.get());
        assertEquals(0, m.disables.get());
    }

    @Test
    void enableDisableAreIdempotentAndToggleConfig() {
        EverlastingnessClient client = EverlastingnessClient.get();
        StubModule m = new StubModule("stub_toggle_" + System.nanoTime());
        client.registerModule(m); // enabled once

        client.disableModule(m.getId()); // -> off
        assertFalse(m.isEnabled());
        assertFalse(client.config().getModuleEnabled(m.getId(), true),
                "config must record the disable even if the module's default is true");

        client.disableModule(m.getId()); // no-op (already off)
        assertEquals(1, m.disables.get());

        client.enableModule(m.getId()); // -> on
        assertTrue(m.isEnabled());
        assertTrue(client.config().getModuleEnabled(m.getId(), false));

        client.enableModule(m.getId()); // no-op (already on)
        assertEquals(2, m.enables.get());
    }

    @Test
    void lookupAndModulesCollectionWork() {
        EverlastingnessClient client = EverlastingnessClient.get();
        StubModule m = new StubModule("stub_lookup_" + System.nanoTime());
        client.registerModule(m);

        Module found = client.module(m.getId());
        assertNotNull(found, "module() must find a registered module");
        assertEquals(m.getId(), found.getId());

        // The collection view must include the module we just registered.
        assertTrue(client.modules().stream().anyMatch(x -> x.getId().equals(m.getId())));
    }

    @Test
    void unknownModuleLookupReturnsNull() {
        EverlastingnessClient client = EverlastingnessClient.get();
        assertNull(client.module("definitely_not_registered_" + System.nanoTime()));
    }

    @Test
    void eventsBusIsWired() {
        EverlastingnessClient client = EverlastingnessClient.get();
        assertNotNull(client.events(),
                "the client singleton must expose a working EventBus");
        assertEquals("test", client.minecraftVersion());
    }
}

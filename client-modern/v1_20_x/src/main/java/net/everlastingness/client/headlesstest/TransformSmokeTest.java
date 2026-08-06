package net.everlastingness.client.headlesstest;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ServiceLoader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import net.everlastingness.client.headlesstest.fixtures.StubTarget;

/**
 * Verifies Mixin's REAL bytecode-transform pipeline against a stub "MC" class —
 * the closest thing to the live-MC path that can run headless (no GPU/MC/auth).
 *
 * <p><b>Status (WIP — not yet green):</b> the host bootstrap and phase transition
 * both succeed, but {@code Mixins.addConfiguration} currently fails because
 * {@code MixinConfig.onLoad} resolves a {@code null} environment — Mixin's
 * config-load path expects the full host lifecycle (the thing ModLauncher /
 * Fabric provide) to have established a current environment beyond a bare
 * {@code gotoPhase(DEFAULT)}. Reproducing that lifecycle headless is itself
 * substantial work, so this test is committed as a documented WIP rather than a
 * passing check. The {@code StandaloneHostSmokeTest} (which IS green) covers
 * the host-bootstrap half that was the original Phase 3b risk.</p>
 *
 * <p>Intended flow once the lifecycle is reproduced:</p>
 * <ol>
 *   <li>Bootstrap the standalone Mixin host (same path the smoke test checks).</li>
 *   <li>Register the {@code headless-test.mixins.json} config, which applies
 *       {@code MixinStubTarget} to {@link StubTarget}.</li>
 *   <li>Read StubTarget's original bytecode from the classpath.</li>
 *   <li>Run it through {@code IMixinTransformer.transformClassBytes(...)} — the
 *       exact method the agent's ClassFileTransformer calls per class at runtime.</li>
 *   <li>Load the transformed bytes in a private ClassLoader, call {@code greet()},
 *       and assert: (a) the mixin's {@code System.out.println("MIXIN-APPLIED")}
 *       fired, and (b) greet() still returned the vanilla string.</li>
 * </ol>
 *
 * <p>Green = Mixin actually rewrote the target class's bytecode through our host.
 * This is direct evidence of the transform half of the standalone host — the
 * only part still tagged "needs real MC" was the application to obfuscated MC
 * classes specifically, which this stub proves out structurally.</p>
 *
 * <p>Run:</p>
 * <pre>
 * java -cp everlastingness-1.20.1-1.0.0-agent.jar \
 *      net.everlastingness.client.headlesstest.TransformSmokeTest
 * </pre>
 */
public final class TransformSmokeTest {

    public static void main(String[] args) throws Exception {
        int failures = 0;

        // 1) Bootstrap the host exactly as the agent premain does.
        try {
            Class<?> bootstrap = Class.forName("org.spongepowered.asm.launch.MixinBootstrap");
            Method init = bootstrap.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(null);
            System.out.println("PASS  Mixin host bootstrapped");
        } catch (Throwable t) {
            failures++;
            System.out.println("FAIL  Mixin host bootstrap  ->  " + t);
            t.printStackTrace();
            System.exit(1);
        }

        // 2) Transition MixinEnvironment to the DEFAULT phase so mixin configs
        //    can bind an environment during onLoad (otherwise env is null and
        //    config creation fails). gotoPhase is package-private; reflect it.
        try {
            Class<?> envCls = Class.forName("org.spongepowered.asm.mixin.MixinEnvironment");
            Class<?> phaseCls = Class.forName("org.spongepowered.asm.mixin.MixinEnvironment$Phase");
            java.lang.reflect.Field defaultPhase = phaseCls.getField("DEFAULT");
            java.lang.reflect.Method gotoPhase = envCls.getDeclaredMethod("gotoPhase", phaseCls);
            gotoPhase.setAccessible(true);
            gotoPhase.invoke(null, defaultPhase.get(null));
            System.out.println("PASS  MixinEnvironment transitioned to DEFAULT phase");
        } catch (Throwable t) {
            failures++;
            System.out.println("FAIL  transitioning to DEFAULT phase  ->  " + t);
            t.printStackTrace();
            System.exit(1);
        }

        // 3) Register the stub mixin config.
        try {
            Mixins.addConfiguration("headless-test.mixins.json");
            System.out.println("PASS  mixin config registered (headless-test.mixins.json)");
        } catch (Throwable t) {
            failures++;
            System.out.println("FAIL  registering mixin config  ->  " + t);
            t.printStackTrace();
            System.exit(1);
        }

        // 4) Get the active IMixinTransformer from the default environment.
        Object env = org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment();
        Object active = env.getClass().getMethod("getActiveTransformer").invoke(env);
        if (active == null || !(active instanceof IMixinTransformer)) {
            failures++;
            System.out.println("FAIL  no active IMixinTransformer after bootstrap/config");
            System.out.println();
            System.out.println("==> TransformSmokeTest: " + failures + " CHECK(S) FAILED");
            System.exit(1);
        }
        IMixinTransformer transformer = (IMixinTransformer) active;
        System.out.println("PASS  active IMixinTransformer present");

        // 4) Read StubTarget's original bytecode.
        String targetInternal = StubTarget.class.getName().replace('.', '/');
        byte[] original;
        try (InputStream in = StubTarget.class.getClassLoader()
                .getResourceAsStream(targetInternal + ".class")) {
            if (in == null) {
                throw new IllegalStateException("StubTarget bytecode not on classpath");
            }
            original = in.readAllBytes();
        }

        // 5) Transform it.
        byte[] transformed = transformer.transformClassBytes(
                StubTarget.class.getName(), StubTarget.class.getName(), original);
        if (transformed == null || transformed.length == original.length
                && java.util.Arrays.equals(transformed, original)) {
            // Mixin returns the (possibly unchanged) bytes; if it's byte-identical
            // the mixin didn't apply — that's the failure mode we care about.
            failures++;
            System.out.println("FAIL  transformClassBytes returned unchanged bytes — mixin did not apply");
        } else {
            System.out.println("PASS  transformClassBytes rewrote the target ("
                    + original.length + " -> " + transformed.length + " bytes)");
        }

        // 6) Load and invoke to confirm behaviour end-to-end. Capture stdout to
        //    assert the injected println fired.
        if (failures == 0) {
            try {
                Class<?> loaded = new SingleClassLoader(targetInternal, transformed).loadClass(
                        StubTarget.class.getName());
                Object instance = loaded.getDeclaredConstructor().newInstance();
                Method greet = loaded.getMethod("greet");

                PrintStream realOut = System.out;
                java.io.ByteArrayOutputStream capture = new java.io.ByteArrayOutputStream();
                System.setOut(new PrintStream(capture));
                String result;
                try {
                    result = (String) greet.invoke(instance);
                } finally {
                    System.setOut(realOut);
                }
                String printed = capture.toString().trim();

                if (!"MIXIN-APPLIED".equals(printed)) {
                    failures++;
                    System.out.println("FAIL  injected println did not fire (stdout was: \""
                            + printed + "\")");
                } else {
                    System.out.println("PASS  injected code ran: stdout=\"" + printed + "\"");
                }
                if (!"vanilla-greet".equals(result)) {
                    failures++;
                    System.out.println("FAIL  greet() returned \"" + result
                            + "\" (expected \"vanilla-greet\")");
                } else {
                    System.out.println("PASS  greet() returned the vanilla string (\"" + result + "\")");
                }
            } catch (Throwable t) {
                failures++;
                System.out.println("FAIL  loading/invoking transformed class  ->  " + t);
                t.printStackTrace();
            }
        }

        System.out.println();
        if (failures == 0) {
            System.out.println("==> TransformSmokeTest: ALL GREEN");
            System.exit(0);
        }
        System.out.println("==> TransformSmokeTest: " + failures + " CHECK(S) FAILED");
        System.exit(1);
    }

    /** Minimal ClassLoader that defines one class from a byte array. */
    private static final class SingleClassLoader extends ClassLoader {
        private final String name;
        private final byte[] bytes;
        SingleClassLoader(String name, byte[] bytes) { this.name = name; this.bytes = bytes; }
        @Override
        protected Class<?> findClass(String cn) throws ClassNotFoundException {
            if (cn.equals(name)) {
                return defineClass(cn, bytes, 0, bytes.length);
            }
            return super.findClass(cn);
        }
    }

    private TransformSmokeTest() {}
}

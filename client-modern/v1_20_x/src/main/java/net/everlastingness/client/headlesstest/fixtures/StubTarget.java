package net.everlastingness.client.headlesstest.fixtures;

/**
 * A stand-in "Minecraft class" for the bytecode-transform test. It mimics what
 * a real obfuscated MC target looks like to Mixin: a plain class with a method
 * the mixin injects into. The transform test loads this class's bytecode,
 * runs it through Mixin, and verifies the injection changed the output.
 *
 * <p>The {@link #greet()} method returns a fixed vanilla string. A mixin
 * ({@code MixinStubTarget}) injects a System.out print at its HEAD; after a
 * successful transform, invoking greet() prints the mixin's marker line.</p>
 */
public class StubTarget {
    public String greet() {
        return "vanilla-greet";
    }
}

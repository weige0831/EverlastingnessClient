package net.everlastingness.client.headlesstest.fixtures;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin against {@link StubTarget}, the stand-in for an obfuscated MC class.
 *
 * <p>Injects at the HEAD of {@code greet()} and prints a marker line. After the
 * transform test runs this mixin through Mixin's real transformer, calling
 * {@code greet()} on the transformed class will print "MIXIN-APPLIED" before
 * returning the vanilla string — proving the injection landed.</p>
 *
 * <p>Note {@code remap = false}: StubTarget is not a Minecraft class, so there
 * is no refmap/remap to apply. This keeps the transform purely about our stub.</p>
 */
@Mixin(value = StubTarget.class, remap = false)
public class MixinStubTarget {

    @Inject(method = "greet()Ljava/lang/String;", at = @At("HEAD"))
    private void everlastingness$mark(CallbackInfoReturnable<String> cir) {
        System.out.println("MIXIN-APPLIED");
    }
}

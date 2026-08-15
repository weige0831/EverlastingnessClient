package net.everlastingness.client.v1_20_x.mixin.shulker;

import net.everlastingness.client.common.EverlastingnessClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinGuiScreenShulkerPreview {
    @Inject(method = "renderTooltip(Lnet/minecraft/item/ItemStack;II)V", at = @At("HEAD"), require = 0)
    private void everlastingness$shulkerPreview(ItemStack stack, int x, int y, CallbackInfo ci) {
        try {
            EverlastingnessClient c = EverlastingnessClient.get();
            if (c == null || stack == null) return;
            var m = c.module("shulker_preview");
            if (m == null || !m.isEnabled()) return;
            // NBT API surface changed in 1.21.5 (NbtCompound method renames);
            // walk it entirely via reflection so one source compiles everywhere.
            Object tagObj;
            try {
                tagObj = ItemStack.class.getMethod("getNbt").invoke(stack);
            } catch (NoSuchMethodException e) {
                tagObj = ItemStack.class.getMethod("getOrCreateNbt").invoke(stack);
            }
            if (tagObj == null) return;
            Object bet = call(tagObj, "getCompound", String.class, "BlockEntityTag");
            if (bet == null) return;
            Object items = call(bet, "getList", new Class[] { String.class, int.class }, "Items", 10);
            if (items == null) return;
            int size = (Integer) call(items, "size", null);
            int total = 0;
            for (int i = 0; i < size; i++) {
                Object slotNbt = call(items, "getCompound", int.class, i);
                if (slotNbt == null) continue;
                Object cnt = call(slotNbt, "getByte", String.class, "Count");
                if (cnt instanceof Byte) total += (Byte) cnt;
            }
            if (total > 0) {
                System.out.println("[Everlastingness] Shulker preview: "
                        + "shulker box contains " + total + " items");
            }
        } catch (Throwable ignored) { }
    }

    private static Object call(Object target, String method, Class<?>[] types, Object... args) {
        try {
            if (types == null) return target.getClass().getMethod(method).invoke(target);
            return target.getClass().getMethod(method, types).invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object call(Object target, String method, Class<?> type, Object arg) {
        try {
            return target.getClass().getMethod(method, type).invoke(target, arg);
        } catch (Throwable t) {
            return null;
        }
    }
}

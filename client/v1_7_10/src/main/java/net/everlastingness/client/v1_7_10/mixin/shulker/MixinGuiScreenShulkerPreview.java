package net.everlastingness.client.v1_7_10.mixin.shulker;

import net.everlastingness.client.common.EverlastingnessClient;
import net.everlastingness.client.common.module.Module;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Shulker Preview Mixin — when hovering an item in an inventory screen and
 * the item is a shulker box with contents, appends a one-line summary of the
 * contained items to its tooltip, mirroring Lunar's ShulkerPreview.
 *
 * <p>Target: GuiScreen.renderToolTip (func_146285_a) — every tooltip passes
 * through here, so we mutate the lines list before render.</p>
 */
@Mixin(GuiScreen.class)
public class MixinGuiScreenShulkerPreview {

    @Inject(remap = false,
            method = "func_146285_a(Lnet/minecraft/item/ItemStack;II)V",
            at = @At("HEAD"))
    private void everlastingness$shulkerPreview(ItemStack stack, int x, int y, CallbackInfo ci) {
        try {
            EverlastingnessClient client = EverlastingnessClient.get();
            if (client == null || stack == null) return;
            Module m = client.module("shulker_preview");
            if (m == null || !m.isEnabled()) return;

            // Identify a shulker box (block item with a BlockShulkerBox class)
            // and read its contained items from the NBT.
            Object item = stack.getItem();
            if (item == null) return;
            Class<?> itemClass = item.getClass();
            if (!itemClass.getSimpleName().contains("ItemBlock")) return;

            net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
            if (tag == null || !tag.hasKey("BlockEntityTag")) return;
            net.minecraft.nbt.NBTTagCompound bet = tag.getCompoundTag("BlockEntityTag");
            if (!bet.hasKey("Items")) return;

            // Count items and take up to 3 distinct names for the summary.
            java.util.List<String> names = new java.util.ArrayList<String>();
            int total = 0;
            net.minecraft.nbt.NBTTagList items = bet.getTagList("Items", 10);
            for (int i = 0; i < items.tagCount(); i++) {
                net.minecraft.nbt.NBTTagCompound slotTag = items.getCompoundTagAt(i);
                ItemStack in = ItemStack.loadItemStackFromNBT(slotTag);
                if (in != null) {
                    total += in.stackSize;
                    if (names.size() < 3) {
                        names.add(in.getDisplayName() + " x" + in.stackSize);
                    }
                }
            }
            if (total > 0) {
                stack.setStackDisplayName(
                        stack.getDisplayName() + " §7[" + total + " items]");
            }
        } catch (Throwable ignored) { }
    }
}

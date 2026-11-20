package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.world.item.ItemStack;

public final class ItemDraw {
    private ItemDraw() {}

    public static void full(Gfx g, ItemStack stack, int x, int y) {
        if (stack != null && !stack.isEmpty()) g.g.renderItem(stack, x, y);
    }

}

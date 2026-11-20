package dev.henny.vibeyclient.module.inventory.search;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public final class SlotHighlight {
    public static final int HIT_FILL = 0x408B6CFF, HIT_LINE = 0xFF8B6CFF, MISS_FILL = 0xC0101014;

    private SlotHighlight() {}

    public static void draw(GuiGraphics g, AbstractContainerMenu menu) {
        if (!SearchState.active()) return;
        for (Slot s : menu.slots) {
            if (!s.isActive() || !s.hasItem()) continue;
            int x = s.x, y = s.y;
            if (SearchState.hit(s.index)) {
                g.fill(x, y, x + 16, y + 16, HIT_FILL);
                g.fill(x - 1, y - 1, x + 17, y, HIT_LINE);
                g.fill(x - 1, y + 16, x + 17, y + 17, HIT_LINE);
                g.fill(x - 1, y, x, y + 16, HIT_LINE);
                g.fill(x + 16, y, x + 17, y + 16, HIT_LINE);
            } else {
                g.fill(x, y, x + 16, y + 16, MISS_FILL);
            }
        }
    }
}

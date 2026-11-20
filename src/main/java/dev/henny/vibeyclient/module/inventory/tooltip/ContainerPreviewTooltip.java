package dev.henny.vibeyclient.module.inventory.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

public final class ContainerPreviewTooltip implements ClientTooltipComponent {
    private static final int CELL = 18, CELL_BG = 0x66000000, CELL_LINE = 0x22FFFFFF;

    private final ContainerPreview preview;

    public ContainerPreviewTooltip(ContainerPreview preview) {
        this.preview = preview;
    }

    @Override
    public int getHeight(Font font) {
        return preview.rows() * CELL + 4;
    }

    @Override
    public int getWidth(Font font) {
        return preview.columns() * CELL + 1;
    }

    @Override
    public void renderImage(Font font, int x, int y, int w, int h, GuiGraphics g) {
        int top = y + 2;
        g.fill(x, y, x + getWidth(font), y + 1, preview.tint());
        for (int i = 0; i < preview.items().size(); i++) {
            int cx = x + (i % preview.columns()) * CELL, cy = top + (i / preview.columns()) * CELL;
            g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, CELL_BG);
            g.fill(cx, cy + CELL - 1, cx + CELL - 1, cy + CELL, CELL_LINE);
            ItemStack st = preview.items().get(i);
            if (st.isEmpty()) continue;
            g.renderItem(st, cx + 1, cy + 1);
            g.renderItemDecorations(font, st, cx + 1, cy + 1);
        }
    }
}

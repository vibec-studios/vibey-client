package dev.henny.vibeyclient.ui.vui;

import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;

public final class HostGfx {
    private HostGfx() {}

    public static Gfx of(Object graphics) {
        //? if >=26.1 {
        /*return new Gfx((net.minecraft.client.gui.GuiGraphicsExtractor) graphics, Minecraft.getInstance().font);
        *///?} else
        return new Gfx((net.minecraft.client.gui.GuiGraphics) graphics, Minecraft.getInstance().font);
    }
}

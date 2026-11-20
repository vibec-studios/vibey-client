package dev.henny.r3.mc.hud;

import dev.henny.r3.mc.client.R3Client;
import dev.henny.r3.mc.compat.Game;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;

public final class HintOverlay {
    public static final int BELOW_CROSSHAIR = 22;
    public static final int BACKGROUND = 0xA0101014;
    public static final int TEXT = 0xFFF4F4F5;

    private HintOverlay() {}

    //? if >=26.1 {
    /*public static void draw(GuiGraphicsExtractor g) {
    *///?} else
    public static void draw(GuiGraphics g) {
        if (!R3Client.started() || Game.hudHidden()) return;
        String hint = R3Client.get().hint();
        if (hint == null || hint.isEmpty()) return;
        Font font = Game.mc().font;
        int w = font.width(hint);
        int x = (g.guiWidth() - w) / 2;
        int y = g.guiHeight() / 2 + BELOW_CROSSHAIR;
        g.fill(x - 4, y - 3, x + w + 4, y + font.lineHeight + 2, BACKGROUND);
        //? if >=26.1 {
        /*g.text(font, hint, x, y, TEXT, false);
        *///?} else
        g.drawString(font, hint, x, y, TEXT, false);
    }
}

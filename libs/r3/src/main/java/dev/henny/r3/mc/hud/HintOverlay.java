package dev.henny.r3.mc.hud;

import dev.henny.r3.hint.HintPlacement;
import dev.henny.r3.interact.Interactable;
import dev.henny.r3.interact.Interaction;
import dev.henny.r3.mc.client.R3Client;
import dev.henny.r3.mc.compat.Game;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;

public final class HintOverlay {
    public static final int GAP = 10;
    public static final int BOTTOM_MARGIN = 44;
    public static final float SCALE = 0.75f;
    public static final int BACKGROUND = 0x90101014;
    public static final int TEXT = 0xFFE4E4E7;

    private static final HintPlacement placement = new HintPlacement();

    private HintOverlay() {}

    //? if >=26.1 {
    /*public static void draw(GuiGraphicsExtractor g) {
    *///?} else
    public static void draw(GuiGraphics g) {
        if (!R3Client.started() || Game.hudHidden()) return;
        String hint = R3Client.get().hint();
        if (hint == null || hint.isEmpty()) return;
        Font font = Game.mc().font;
        int h = g.guiHeight();
        int lineHeight = Math.round((font.lineHeight + 4) * SCALE);
        int y = HintPlacement.lineY(h / 2, h, lineHeight, targetBottom(), GAP, BOTTOM_MARGIN);
        float w = font.width(hint) * SCALE;
        float x = (g.guiWidth() - w) / 2f;
        var pose = g.pose();
        //? if >=1.21.6 {
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(SCALE, SCALE);
        //?} else {
        /*pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(SCALE, SCALE, 1);
        *///?}
        g.fill(-3, -2, font.width(hint) + 3, font.lineHeight + 1, BACKGROUND);
        //? if >=26.1 {
        /*g.text(font, hint, 0, 0, TEXT, false);
        *///?} else
        g.drawString(font, hint, 0, 0, TEXT, false);
        //? if >=1.21.6 {
        pose.popMatrix();
        //?} else
        /*pose.popPose();*/
    }

    private static double targetBottom() {
        Interaction interaction = R3Client.get().scene().interaction();
        Interactable t = interaction.held() != null ? interaction.held() : interaction.hover().target();
        if (t == null) return Double.NaN;
        return placement.bottomOf(t.transform(), R3Client.get().camera(), Game.fovDegrees(), Game.aspect());
    }
}

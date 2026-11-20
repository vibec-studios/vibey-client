package dev.henny.vibeyclient.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;

/**
 * Thin drawing layer over GuiGraphics (1.21) / GuiGraphicsExtractor (26), so screens and HUD code never touch the
 * per-version drawing API. Colors are always ARGB. Ported from Vibey Lyrics.
 */
public final class Gfx {
    //? if >=26.1 {
    /*public final GuiGraphicsExtractor g;
    *///?} else
    public final GuiGraphics g;
    public final Font font;

    //? if >=26.1 {
    /*public Gfx(GuiGraphicsExtractor g, Font font) {
    *///?} else
    public Gfx(GuiGraphics g, Font font) {
        this.g = g;
        this.font = font;
    }

    public static Gfx of(Object graphics) {
        //? if >=26.1 {
        /*return new Gfx((GuiGraphicsExtractor) graphics, net.minecraft.client.Minecraft.getInstance().font);
        *///?} else
        return new Gfx((GuiGraphics) graphics, net.minecraft.client.Minecraft.getInstance().font);
    }

    public int screenWidth() {
        return g.guiWidth();
    }

    public int screenHeight() {
        return g.guiHeight();
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        if (x2 <= x1 || y2 <= y1 || (color >>> 24) == 0) return;
        g.fill(x1, y1, x2, y2, color);
    }

    public void rect(int x, int y, int w, int h, int color) {
        fill(x, y, x + w, y + h, color);
    }

    /** 1px frame. */
    public void outline(int x, int y, int w, int h, int color) {
        fill(x, y, x + w, y + 1, color);
        fill(x, y + h - 1, x + w, y + h, color);
        fill(x, y + 1, x + 1, y + h - 1, color);
        fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public void text(String s, int x, int y, int color) {
        //? if >=26.1 {
        /*g.text(font, s, x, y, color, false);
        *///?} else
        g.drawString(font, s, x, y, color, false);
    }

    /** Text with the vanilla drop shadow (HUD widgets). */
    public void textShadow(String s, int x, int y, int color) {
        if ((color >>> 24) < 6) return;
        //? if >=26.1 {
        /*g.text(font, s, x, y, color, true);
        *///?} else
        g.drawString(font, s, x, y, color, true);
    }

    public void text(Component c, int x, int y, int color) {
        // Very low alpha would be drawn fully opaque by older versions.
        if ((color >>> 24) < 6) return;
        //? if >=26.1 {
        /*g.text(font, c, x, y, color, false);
        *///?} else
        g.drawString(font, c, x, y, color, false);
    }

    /** Text in bold (Minecraft font). */
    public void bold(String s, int x, int y, int color) {
        text(Component.literal(s).withStyle(net.minecraft.ChatFormatting.BOLD), x, y, color);
    }

    public int boldWidth(String s) {
        return font.width(Component.literal(s).withStyle(net.minecraft.ChatFormatting.BOLD));
    }

    /** Text scaled by an integer factor (stays crisp), top left at x, y. */
    public void textScaled(String s, int x, int y, int scale, int color, boolean bold) {
        push();
        translate(x, y);
        scale(scale, scale);
        if (bold) bold(s, 0, 0, color);
        else text(s, 0, 0, color);
        pop();
    }

    public void hline(int x, int y, int w, int color) {
        fill(x, y, x + w, y + 1, color);
    }

    public void vline(int x, int y, int h, int color) {
        fill(x, y, x + 1, y + h, color);
    }

    public int lineHeight() {
        return font.lineHeight;
    }

    public void textRight(String s, int right, int y, int color) {
        text(s, right - font.width(s), y, color);
    }

    public void textCenter(String s, int cx, int y, int color) {
        text(s, cx - font.width(s) / 2, y, color);
    }

    public int width(String s) {
        return font.width(s);
    }

    public String clip(String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        return font.plainSubstrByWidth(s, Math.max(0, maxWidth - font.width("…"))) + "…";
    }

    /** A whole square texture of {@code texSize} pixels, drawn into {@code w × h}. */
    //? if >=1.21.11 {
    public void image(net.minecraft.resources.Identifier tex, int x, int y, int w, int h, int texSize) {
    //?} else
    /*public void image(net.minecraft.resources.ResourceLocation tex, int x, int y, int w, int h, int texSize) {*/
        image(tex, x, y, w, h, 0, 0, texSize, texSize, texSize, texSize);
    }

    /** The part (u, v, uw × vh) of a {@code texW × texH} texture, drawn into {@code w × h}. */
    //? if >=1.21.11 {
    public void image(net.minecraft.resources.Identifier tex, int x, int y, int w, int h, float u, float v, int uw, int vh, int texW, int texH) {
    //?} else
    /*public void image(net.minecraft.resources.ResourceLocation tex, int x, int y, int w, int h, float u, float v, int uw, int vh, int texW, int texH) {*/
        //? if >=1.21.6 {
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, uw, vh, texW, texH);
        //?} else if >=1.21.2 {
        /*g.blit(net.minecraft.client.renderer.RenderType::guiTextured, tex, x, y, u, v, w, h, uw, vh, texW, texH);
        *///?} else {
        /*// 1.21.1 blits without blending: soft alpha (thumbnail glows, icons) would turn opaque
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.blit(tex, x, y, w, h, u, v, uw, vh, texW, texH);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        *///?}
    }

    public static final int CURSOR_ARROW = 0, CURSOR_HAND = 1, CURSOR_MOVE = 2, CURSOR_RESIZE_NS = 3,
            CURSOR_RESIZE_EW = 4, CURSOR_NOT_ALLOWED = 5;

    /** Asks for a mouse cursor shape for this frame (1.21.9+; older versions keep the arrow). */
    public void cursor(int kind) {
        //? if >=1.21.9 {
        g.requestCursor(switch (kind) {
            case CURSOR_HAND -> com.mojang.blaze3d.platform.cursor.CursorTypes.POINTING_HAND;
            case CURSOR_MOVE -> com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_ALL;
            case CURSOR_RESIZE_NS -> com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_NS;
            case CURSOR_RESIZE_EW -> com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_EW;
            case CURSOR_NOT_ALLOWED -> com.mojang.blaze3d.platform.cursor.CursorTypes.NOT_ALLOWED;
            default -> com.mojang.blaze3d.platform.cursor.CursorTypes.ARROW;
        });
        //?}
    }

    public void scissor(int x1, int y1, int x2, int y2) {
        g.enableScissor(x1, y1, x2, y2);
    }

    public void endScissor() {
        g.disableScissor();
    }

    public void widget(AbstractWidget w, int mouseX, int mouseY, float delta) {
        //? if >=26.1 {
        /*w.extractRenderState(g, mouseX, mouseY, delta);
        *///?} else
        w.render(g, mouseX, mouseY, delta);
    }

    /**
     * Starts a new layer: everything drawn after it is on top of everything before (1.21.6+ draws text after shapes
     * within a layer, so overlays, popups and tooltips need this).
     */
    public void layer() {
        //? if >=1.21.6 {
        g.nextStratum();
        //?} else
        /*g.flush();*/
    }

    // -------------------------------------------------------------------------------------------- transforms

    public void push() {
        //? if >=1.21.6 {
        g.pose().pushMatrix();
        //?} else
        /*g.pose().pushPose();*/
    }

    public void pop() {
        //? if >=1.21.6 {
        g.pose().popMatrix();
        //?} else
        /*g.pose().popPose();*/
    }

    public void translate(float x, float y) {
        //? if >=1.21.6 {
        g.pose().translate(x, y);
        //?} else
        /*g.pose().translate(x, y, 0f);*/
    }

    /** Turns everything drawn after it by {@code radians} (clockwise on screen). */
    public void rotate(float radians) {
        //? if >=1.21.6 {
        g.pose().rotate(radians);
        //?} else
        /*g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(radians));*/
    }

    public void scale(float sx, float sy) {
        //? if >=1.21.6 {
        g.pose().scale(sx, sy);
        //?} else
        /*g.pose().scale(sx, sy, 1f);*/
    }
}

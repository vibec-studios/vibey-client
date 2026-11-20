package dev.henny.vui.mc.draw;

import dev.henny.vui.render.Cursor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;

final class GuiTarget {
    //? if >=26.1 {
    /*private GuiGraphicsExtractor g;
    *///?} else
    private GuiGraphics g;
    //? if >=1.21.6 {
    private final org.joml.Matrix3x2f base = new org.joml.Matrix3x2f();
    //?} else
    /*private final org.joml.Matrix4f base = new org.joml.Matrix4f();*/
    private Font font;
    private int depth;

    void bind(Object graphics, Font font) {
        //? if >=26.1 {
        /*this.g = (GuiGraphicsExtractor) graphics;
        *///?} else
        this.g = (GuiGraphics) graphics;
        this.font = font;
        depth = 0;
        //? if >=1.21.6 {
        base.set(g.pose());
        //?} else
        /*base.set(g.pose().last().pose());*/
    }

    void fill(int x1, int y1, int x2, int y2, int argb) {
        g.fill(x1, y1, x2, y2, argb);
    }

    void gradient(int x1, int y1, int x2, int y2, int from, int to) {
        g.fillGradient(x1, y1, x2, y2, from, to);
    }

    void text(String s, int x, int y, int argb, boolean shadow) {
        //? if >=26.1 {
        /*g.text(font, s, x, y, argb, shadow);
        *///?} else
        g.drawString(font, s, x, y, argb, shadow);
    }

    void text(Component c, int x, int y, int argb, boolean shadow) {
        //? if >=26.1 {
        /*g.text(font, c, x, y, argb, shadow);
        *///?} else
        g.drawString(font, c, x, y, argb, shadow);
    }

    void blit(Object id, int x, int y, int w, int h, float u, float v, int uw, int vh, int tw, int th, int tint) {
        //? if >=1.21.11 {
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, (net.minecraft.resources.Identifier) id, x, y, u, v, w, h, uw, vh, tw, th, tint);
        //?} else if >=1.21.6 {
        /*g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, (net.minecraft.resources.ResourceLocation) id, x, y, u, v, w, h, uw, vh, tw, th, tint);
        *///?} else if >=1.21.2 {
        /*g.blit(net.minecraft.client.renderer.RenderType::guiTextured, (net.minecraft.resources.ResourceLocation) id, x, y, u, v, w, h, uw, vh, tw, th, tint);
        *///?} else {
        /*com.mojang.blaze3d.systems.RenderSystem.setShaderColor(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f, (tint >>> 24) / 255f);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.blit((net.minecraft.resources.ResourceLocation) id, x, y, w, h, u, v, uw, vh, tw, th);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        *///?}
    }

    void scissor(int x1, int y1, int x2, int y2) {
        pushBase();
        g.enableScissor(x1, y1, x2, y2);
        pop();
    }

    void endScissor() {
        g.disableScissor();
    }

    void push() {
        depth++;
        //? if >=1.21.6 {
        g.pose().pushMatrix();
        //?} else
        /*g.pose().pushPose();*/
    }

    void pushBase() {
        push();
        //? if >=1.21.6 {
        g.pose().set(base);
        //?} else
        /*g.pose().last().pose().set(base);*/
    }

    void pop() {
        if (depth == 0) return;
        depth--;
        //? if >=1.21.6 {
        g.pose().popMatrix();
        //?} else
        /*g.pose().popPose();*/
    }

    void unwind() {
        while (depth > 0) pop();
    }

    void translate(float x, float y) {
        //? if >=1.21.6 {
        g.pose().translate(x, y);
        //?} else
        /*g.pose().translate(x, y, 0f);*/
    }

    void scale(float sx, float sy) {
        //? if >=1.21.6 {
        g.pose().scale(sx, sy);
        //?} else
        /*g.pose().scale(sx, sy, 1f);*/
    }

    void rotate(float radians) {
        //? if >=1.21.6 {
        g.pose().rotate(radians);
        //?} else
        /*g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(radians));*/
    }

    void layer() {
        //? if >=1.21.6 {
        g.nextStratum();
        //?} else
        /*g.flush();*/
    }

    void cursor(Cursor cursor) {
        //? if >=1.21.9 {
        g.requestCursor(CursorShapes.of(cursor));
        //?}
    }
}

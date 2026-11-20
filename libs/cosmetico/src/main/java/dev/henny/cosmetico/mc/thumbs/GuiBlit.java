package dev.henny.cosmetico.mc.thumbs;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;

final class GuiBlit {
    private GuiBlit() {}

    //? if >=26.1 {
    /*static void blit(GuiGraphicsExtractor g, net.minecraft.resources.Identifier tex, int x, int y, int w, int h, int texW, int texH) {
    *///?} else if >=1.21.11 {
    static void blit(GuiGraphics g, net.minecraft.resources.Identifier tex, int x, int y, int w, int h, int texW, int texH) {
    //?} else
    /*static void blit(GuiGraphics g, net.minecraft.resources.ResourceLocation tex, int x, int y, int w, int h, int texW, int texH) {*/
        //? if >=1.21.6 {
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, w, h, texW, texH, texW, texH);
        //?} else if >=1.21.2 {
        /*g.blit(net.minecraft.client.renderer.RenderType::guiTextured, tex, x, y, 0f, 0f, w, h, texW, texH, texW, texH);
        *///?} else {
        /*com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.blit(tex, x, y, w, h, 0f, 0f, texW, texH, texW, texH);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        *///?}
    }
}

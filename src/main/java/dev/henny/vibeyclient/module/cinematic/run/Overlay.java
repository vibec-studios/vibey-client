package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.concurrent.ThreadLocalRandom;

public final class Overlay {
    public static final double BAR = 0.115;
    private static final Identifier VIGNETTE = Identifier.fromNamespaceAndPath("vibeyclient", "textures/cinematic/vignette.png");
    private static final Identifier GRAIN = Identifier.fromNamespaceAndPath("vibeyclient", "textures/cinematic/grain.png");
    private static final int GRAIN_SIZE = 128;

    private Overlay() {}

    public static int barHeight(int screenH, double amount) {
        return (int) Math.round(screenH * BAR * amount);
    }

    static void fade(Gfx g, double alpha) {
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        if (a > 0) g.rect(0, 0, g.screenWidth(), g.screenHeight(), a << 24);
    }

    public static void bars(Gfx g, double amount) {
        int h = barHeight(g.screenHeight(), amount);
        if (h <= 0) return;
        g.rect(0, 0, g.screenWidth(), h, 0xFF000000);
        g.rect(0, g.screenHeight() - h, g.screenWidth(), h, 0xFF000000);
    }

    static void vignette(GuiGraphics g, Gfx gfx, double amount) {
        int a = (int) Math.round(Math.max(0, Math.min(1, amount)) * 255);
        if (a <= 0) return;
        g.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, 0, 0, gfx.screenWidth(), gfx.screenHeight(), 256, 256, 256, 256, a << 24 | 0xFFFFFF);
    }

    static void grain(GuiGraphics g, Gfx gfx, double amount) {
        int a = (int) Math.round(Math.max(0, Math.min(1, amount)) * 48);
        if (a <= 0) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int ox = -r.nextInt(GRAIN_SIZE), oy = -r.nextInt(GRAIN_SIZE);
        for (int x = ox; x < gfx.screenWidth(); x += GRAIN_SIZE) {
            for (int y = oy; y < gfx.screenHeight(); y += GRAIN_SIZE) {
                g.blit(RenderPipelines.GUI_TEXTURED, GRAIN, x, y, 0, 0, GRAIN_SIZE, GRAIN_SIZE, GRAIN_SIZE, GRAIN_SIZE, GRAIN_SIZE, GRAIN_SIZE, a << 24 | 0xFFFFFF);
            }
        }
    }
}

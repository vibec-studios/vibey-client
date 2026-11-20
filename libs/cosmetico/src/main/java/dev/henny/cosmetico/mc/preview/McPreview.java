package dev.henny.cosmetico.mc.preview;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.mc.CosmeticoMod;
import dev.henny.cosmetico.mc.boot.CosmeticoBoot;
import dev.henny.cosmetico.mc.compat.McSelf;
import dev.henny.cosmetico.preview.PreviewState;
import dev.henny.cosmetico.preview.PreviewView;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;
import java.util.WeakHashMap;

public final class McPreview {
    private McPreview() {}

    private static final Map<PreviewView, PreviewRenderer> RENDERERS = new WeakHashMap<>();

    public static PreviewView create() {
        CosmeticoBoot.ensure();
        return Cosmetico.preview();
    }

    //? if >=26.1 {
    /*public static void draw(GuiGraphicsExtractor g, PreviewView view, int x, int y, int w, int h, float delta) {
    *///?} else
    public static void draw(GuiGraphics g, PreviewView view, int x, int y, int w, int h, float delta) {
        if (!(view instanceof PreviewState state)) return;
        double now = Cosmetico.engine().clock.now();
        float size = state.frame(x, y, w, h, delta, now, Cosmetico.engine().avatars.peek(McSelf.id()));
        try {
            RENDERERS.computeIfAbsent(view, v -> new PreviewRenderer(state)).render(g, x, y, w, h, size, now);
        } catch (RuntimeException e) {
            CosmeticoMod.LOG.debug("Preview failed: {}", e.toString());
        }
    }
}

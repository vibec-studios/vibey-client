package dev.henny.r3.vui;

import dev.henny.r3.render.TextureHandle;
import dev.henny.r3.support.ImagePanelCanvas;
import dev.henny.vui.headless.Headless;
import dev.henny.vui.headless.ImageCanvas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VuiCanvasTest {
    final Headless headless = Headless.create();
    final ImageCanvas image = new ImageCanvas(64, 32, 1, headless.textures(), Headless.BACKGROUND);
    final ImagePanelCanvas panel = new ImagePanelCanvas(image, 64, 32);

    @Test
    void horizontalGradientRunsLeftToRight() {
        new VuiCanvas(TextureResolver.NONE).target(panel).gradient(0, 0, 64, 32, 0xFFFF0000, 0xFF0000FF, true);
        int left = image.pixel(1, 16);
        int right = image.pixel(62, 16);
        assertTrue((left >> 16 & 0xFF) > (right >> 16 & 0xFF));
        assertTrue((right & 0xFF) > (left & 0xFF));
    }

    @Test
    void nestedAlphaMultiplies() {
        VuiCanvas c = new VuiCanvas(TextureResolver.NONE).target(panel);
        c.pushAlpha(0.5f);
        c.pushAlpha(0.5f);
        assertEquals(0.25f, c.alpha(), 1e-6);
        c.popAlpha();
        c.popAlpha();
        assertEquals(List.of(0.5f, 0.25f, 0.5f, 1f), panel.alphas);
    }

    @Test
    void texturesResolveThroughTheResolverAndNineSliceDrawsNineParts() {
        TextureResolver resolver = id -> id.equals("t:frame") ? TextureHandle.resource(id, 16, 16) : null;
        VuiCanvas c = new VuiCanvas(resolver).target(panel);
        c.texture("t:missing", 0, 0, 8, 8, 0, 0, 1, 1, -1);
        c.nineSlice("t:frame", 0, 0, 40, 20, 4, 16, 16, -1);
        assertEquals(9, panel.textures.size());
    }

    @Test
    void textSlicesAndGlyphsReachThePanel() {
        VuiCanvas c = new VuiCanvas(TextureResolver.NONE).target(panel);
        c.text("hello world", 6, 11, 2, 2, 0xFFFFFFFF, false, false);
        c.glyph('A', 2, 12, 0xFFFFFFFF, false, false);
        assertEquals(List.of("world", "A"), panel.texts);
    }
}

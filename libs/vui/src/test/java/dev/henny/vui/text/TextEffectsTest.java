package dev.henny.vui.text;

import dev.henny.vui.headless.PixelFontMetrics;
import dev.henny.vui.style.StyleError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextEffectsTest {
    private static GlyphState glyph(int index, int count, float time) {
        GlyphState g = new GlyphState();
        g.begin(index, count, 'a', time, index * 6, 6, count * 6, 0xFFFFFFFF);
        return g;
    }

    @Test
    void parsesChainsAndRejectsUnknown() {
        TextEffect e = TextEffects.parse("gradient(#ff0000, #0000ff) wave(2, 1)");
        assertTrue(e instanceof TextEffectChain c && c.size() == 2);
        assertNull(TextEffects.parse("none"));
        StyleError err = assertThrows(StyleError.class, () -> TextEffects.parse("sparkle"));
        assertTrue(err.hint().contains("gradient"));
    }

    @Test
    void gradientSpansGlyphs() {
        TextEffect e = TextEffects.parse("gradient(#ff0000, #0000ff)");
        GlyphState first = glyph(0, 5, 0);
        GlyphState last = glyph(4, 5, 0);
        e.apply(first);
        e.apply(last);
        assertEquals(0xFFFF0000, first.color);
        assertEquals(0xFF0000FF, last.color);
        assertTrue(!e.animated());
    }

    @Test
    void waveMovesOverTime() {
        TextEffect e = TextEffects.parse("wave(2, 1)");
        GlyphState a = glyph(0, 3, 0.25f);
        e.apply(a);
        assertEquals(2f, a.dy, 0.01);
        GlyphState b = glyph(0, 3, 0.75f);
        e.apply(b);
        assertEquals(-2f, b.dy, 0.01);
    }

    @Test
    void rainbowPulseShimmerTypewriter() {
        GlyphState r1 = glyph(0, 3, 0);
        GlyphState r2 = glyph(0, 3, 0.5f);
        TextEffects.parse("rainbow(1)").apply(r1);
        TextEffects.parse("rainbow(1)").apply(r2);
        assertNotEquals(r1.color, r2.color);
        GlyphState p = glyph(0, 1, 0.5f);
        TextEffects.parse("pulse(1, 0.2)").apply(p);
        assertEquals(0.2f, p.alpha, 0.01);
        GlyphState t = glyph(5, 10, 0.1f);
        TextEffects.parse("typewriter(20)").apply(t);
        assertTrue(!t.visible);
        GlyphState t2 = glyph(1, 10, 0.1f);
        TextEffects.parse("typewriter(20)").apply(t2);
        assertTrue(t2.visible);
        GlyphState s = glyph(0, 1, 0);
        s.color = 0xFF000000;
        new dev.henny.vui.text.effect.Shimmer(0xFFFFFFFF, 1, 12).apply(s);
        assertTrue(s.color != 0 );
    }

    @Test
    void glyphRendererDrawsPerGlyphWithOffsets() {
        List<float[]> calls = new ArrayList<>();
        dev.henny.vui.render.Canvas canvas = new RecordingCanvas(calls);
        GlyphRenderer r = new GlyphRenderer();
        float w = r.draw(canvas, new PixelFontMetrics(), "ab c", 0, 4, 10, 20, 0xFFFFFFFF, false, false,
                TextEffects.parse("wave(1, 1)"), 0.25f, 1f);
        assertEquals(3, calls.size());
        assertEquals(27f, w, 0.01);
        assertEquals(10f, calls.get(0)[0], 0.01);
        assertEquals(17f, calls.get(1)[0], 0.01);
        assertTrue(calls.get(0)[1] != 20f);
    }

    private static final class RecordingCanvas extends dev.henny.vui.CountingCanvas {
        private final List<float[]> calls;

        RecordingCanvas(List<float[]> calls) {
            this.calls = calls;
        }

        @Override
        public void glyph(int codepoint, float x, float y, int argb, boolean shadow, boolean bold) {
            calls.add(new float[]{x, y});
        }
    }
}

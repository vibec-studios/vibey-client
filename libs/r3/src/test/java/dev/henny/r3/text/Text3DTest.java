package dev.henny.r3.text;

import dev.henny.r3.draw.TextDrawer;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.TextMetrics;
import dev.henny.r3.support.RecordingCanvas;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Text3DTest {
    private final TextMetrics metrics = TextMetrics.monospace(6, 9);

    @Test
    void layoutCentersEveryLine() {
        GlyphLayout l = new Text3D("abcd\nxy").layout(metrics);
        assertEquals(6, l.count());
        assertEquals(2, l.lineCount());
        assertEquals(24, l.width(), 1e-6);
        assertEquals(18, l.height(), 1e-6);
        assertEquals(-12, l.x(0), 1e-6);
        assertEquals(-6, l.x(4), 1e-6);
        assertEquals(-9, l.y(0), 1e-6);
        assertEquals(0, l.y(4), 1e-6);
    }

    @Test
    void layoutIsCachedUntilTheTextChanges() {
        Text3D t = new Text3D("hi");
        GlyphLayout first = t.layout(metrics);
        assertSame(first, t.layout(metrics));
        t.text("hello");
        assertEquals(5, t.layout(metrics).count());
    }

    @Test
    void waveMovesGlyphsUpAndDown() {
        GlyphState g = new GlyphState().reset(0, 4, 0, 'a', 0, 0, -1);
        GlyphEffects.wave(2f, 1f, 4f).apply(g, 0.25f);
        assertEquals(2f, g.offsetY, 1e-4);
    }

    @Test
    void rainbowAndGradientChangeColorButKeepAlpha() {
        GlyphState g = new GlyphState().reset(0, 2, 0, 'a', 0, 0, 0x80FFFFFF);
        GlyphEffects.rainbow(0f, 0f, 1f).apply(g, 0f);
        assertEquals(0x80FF0000, g.argb);
        g.reset(1, 2, 0, 'b', 0, 0, 0x40FFFFFF);
        GlyphEffects.gradient(0xFF000000, 0xFF0000FF).apply(g, 0f);
        assertEquals(0x400000FF, g.argb);
    }

    @Test
    void typewriterRevealsGlyphByGlyph() {
        GlyphState g = new GlyphState().reset(5, 10, 0, 'a', 0, 0, 0xFFFFFFFF);
        GlyphEffects.typewriter(10f).apply(g, 0.2f);
        assertEquals(0, g.argb >>> 24);
        g.reset(1, 10, 0, 'a', 0, 0, 0xFFFFFFFF);
        GlyphEffects.typewriter(10f).apply(g, 0.5f);
        assertEquals(0xFF, g.argb >>> 24);
    }

    @Test
    void pulseAndShimmer() {
        GlyphState g = new GlyphState().reset(3, 8, 0, 'a', 0, 0, 0xFF808080);
        GlyphEffects.pulse(1f, 0.2f).apply(g, 0.25f);
        assertEquals(1.2f, g.scale, 1e-4);
        assertTrue(g.transformed());
        GlyphEffects.shimmer(1f, 2f, 1f).apply(g, 5f);
        assertNotEquals(0xFF808080, g.argb);
    }

    @Test
    void drawerAddsGlowAndDepthLayers() {
        WorldObject o = new WorldObject("t", "test:text");
        Text3D text = new Text3D("ab").style(TextStyle.DEFAULT.withGlow(0xFF00FFFF, 1f).withDepth(0xFFFFFFFF));
        o.content(text).state().update(true, true, 5, 1f, 0);
        RecordingCanvas canvas = new RecordingCanvas();
        new TextDrawer().draw(canvas, o, text, 0);
        assertEquals(2 * (1 + 16 + TextDrawer.DEPTH_LAYERS), canvas.glyphs);
        assertEquals(0, canvas.depth);
    }

    @Test
    void backgroundIsOneQuad() {
        WorldObject o = new WorldObject("t", "test:text");
        Text3D text = new Text3D("name").style(TextStyle.NAMETAG);
        o.content(text).state().update(true, true, 5, 1f, 0);
        RecordingCanvas canvas = new RecordingCanvas();
        new TextDrawer().draw(canvas, o, text, 0);
        assertEquals(4, canvas.vertices);
        assertEquals(4, canvas.glyphs);
    }

    @Test
    void constantScreenSizeGrowsWithDistance() {
        Transform t = new Transform();
        Text3D text = new Text3D("x").constantScreenSize(0.01f);
        assertEquals(text.worldScale(t, 10) * 2, text.worldScale(t, 20), 1e-6);
        assertEquals(Text3D.DEFAULT_PIXEL_SIZE, new Text3D("x").worldScale(t, 50), 1e-6);
    }

    @Test
    void wrapSplitsOnWords() {
        List<String> out = new ArrayList<>();
        TextWrap.split("one two three four", metrics, 50, out);
        assertEquals(List.of("one two", "three", "four"), out);
    }
}

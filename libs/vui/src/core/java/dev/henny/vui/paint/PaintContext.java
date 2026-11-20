package dev.henny.vui.paint;

import dev.henny.vui.render.Canvas;
import dev.henny.vui.render.TextMetrics;
import dev.henny.vui.render.Textures;
import dev.henny.vui.style.Theme;
import dev.henny.vui.text.GlyphRenderer;

public final class PaintContext {
    private final GlyphRenderer glyphs = new GlyphRenderer();
    private Canvas canvas;
    private TextMetrics metrics;
    private Textures textures;
    private Theme theme;
    private float time;
    private boolean focusVisible;

    public PaintContext begin(Canvas canvas, TextMetrics metrics, Textures textures, Theme theme, float time) {
        this.canvas = canvas;
        this.metrics = metrics;
        this.textures = textures;
        this.theme = theme;
        this.time = time;
        return this;
    }

    public Canvas canvas() {
        return canvas;
    }

    public TextMetrics metrics() {
        return metrics;
    }

    public Textures textures() {
        return textures;
    }

    public Theme theme() {
        return theme;
    }

    public float time() {
        return time;
    }

    public GlyphRenderer glyphs() {
        return glyphs;
    }

    public boolean focusVisible() {
        return focusVisible;
    }

    public void focusVisible(boolean on) {
        focusVisible = on;
    }

    public int token(String name, int fallback) {
        return ThemeColors.get(theme, name, fallback);
    }
}

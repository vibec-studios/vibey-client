package dev.henny.r3.text;

import dev.henny.r3.math.Argb;

public final class GlyphEffects {
    private static final float TAU = (float) (Math.PI * 2);

    private GlyphEffects() {}

    public static GlyphEffect wave(float amplitude, float cyclesPerSecond, float glyphsPerWave) {
        return (g, t) -> g.offsetY += (float) Math.sin((t * cyclesPerSecond + g.index() / glyphsPerWave) * TAU) * amplitude;
    }

    public static GlyphEffect rainbow(float cyclesPerSecond, float huePerGlyph, float saturation) {
        return (g, t) -> g.argb = Argb.hsv(t * cyclesPerSecond + g.index() * huePerGlyph, saturation, 1f, Argb.alpha(g.argb));
    }

    public static GlyphEffect pulse(float cyclesPerSecond, float amount) {
        return (g, t) -> g.scale *= 1f + (float) Math.sin(t * cyclesPerSecond * TAU) * amount;
    }

    public static GlyphEffect shimmer(float glyphsPerSecond, float bandWidth, float boost) {
        return (g, t) -> {
            float span = g.count() + bandWidth * 2f;
            float band = (t * glyphsPerSecond) % span - bandWidth;
            float d = Math.abs(g.index() - band);
            if (d < bandWidth) g.argb = Argb.scaleRgb(g.argb, 1f + boost * (1f - d / bandWidth));
        };
    }

    public static GlyphEffect gradient(int fromArgb, int toArgb) {
        return (g, t) -> {
            int c = Argb.lerp(fromArgb, toArgb, g.progress());
            g.argb = (g.argb & 0xFF000000) | (c & 0xFFFFFF);
        };
    }

    public static GlyphEffect typewriter(float glyphsPerSecond) {
        return (g, t) -> {
            float shown = t * glyphsPerSecond - g.index();
            if (shown < 1f) g.argb = Argb.multiplyAlpha(g.argb, Math.max(0f, shown));
        };
    }
}

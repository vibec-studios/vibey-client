package dev.henny.vui.text.effect;

import dev.henny.vui.anim.Interpolate;
import dev.henny.vui.text.GlyphState;
import dev.henny.vui.text.TextEffect;

public record Gradient(int[] colors, float speed) implements TextEffect {

    @Override
    public void apply(GlyphState g) {
        float t = g.fraction() + g.time * speed;
        if (speed != 0f) t = pingPong(t);
        g.color = keepAlpha(g.color, sample(Interpolate.clamp01(t)));
    }

    @Override
    public boolean animated() {
        return speed != 0f;
    }

    private int sample(float t) {
        if (colors.length == 1) return colors[0];
        float scaled = t * (colors.length - 1);
        int i = Math.min(colors.length - 2, (int) scaled);
        return Interpolate.color(colors[i], colors[i + 1], scaled - i);
    }

    static float pingPong(float t) {
        float m = t % 2f;
        if (m < 0) m += 2f;
        return m > 1f ? 2f - m : m;
    }

    static int keepAlpha(int base, int rgb) {
        return (base & 0xFF000000) | (rgb & 0xFFFFFF);
    }
}

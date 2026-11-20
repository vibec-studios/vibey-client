package dev.henny.vui.text.effect;

import dev.henny.vui.anim.Interpolate;
import dev.henny.vui.text.GlyphState;
import dev.henny.vui.text.TextEffect;

public record Shimmer(int highlight, float speed, float width) implements TextEffect {

    @Override
    public void apply(GlyphState g) {
        float span = g.totalWidth + width * 2f;
        float head = (g.time * speed * span) % (span + width * 4f) - width;
        float center = g.x + g.width * 0.5f;
        float d = Math.abs(center - head);
        if (d >= width) return;
        float strength = 1f - d / width;
        g.color = Gradient.keepAlpha(g.color, Interpolate.color(g.color, highlight, strength));
    }
}

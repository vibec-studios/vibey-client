package dev.henny.vui.text.effect;

import dev.henny.vui.text.GlyphState;
import dev.henny.vui.text.TextEffect;

public record Pulse(float speed, float minAlpha) implements TextEffect {

    @Override
    public void apply(GlyphState g) {
        float wave = (float) (0.5 + 0.5 * Math.cos(g.time * speed * Math.PI * 2));
        g.alpha *= minAlpha + (1f - minAlpha) * wave;
    }
}

package dev.henny.vui.text.effect;

import dev.henny.vui.text.GlyphState;
import dev.henny.vui.text.TextEffect;

public record Wave(float amplitude, float speed, float spread) implements TextEffect {

    @Override
    public void apply(GlyphState g) {
        double phase = g.time * speed * Math.PI * 2 - g.index * spread;
        g.dy += (float) Math.sin(phase) * amplitude;
    }
}

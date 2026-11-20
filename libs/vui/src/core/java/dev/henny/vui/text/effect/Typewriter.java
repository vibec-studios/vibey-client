package dev.henny.vui.text.effect;

import dev.henny.vui.text.GlyphState;
import dev.henny.vui.text.TextEffect;

public record Typewriter(float charsPerSecond, float delay) implements TextEffect {

    @Override
    public void apply(GlyphState g) {
        float shown = (g.time - delay) * charsPerSecond;
        if (g.index >= shown) g.visible = false;
        else if (g.index > shown - 1f) g.alpha *= shown - g.index;
    }
}

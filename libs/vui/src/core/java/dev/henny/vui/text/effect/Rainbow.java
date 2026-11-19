package dev.henny.vui.text.effect;

import dev.henny.vui.text.GlyphState;
import dev.henny.vui.text.TextEffect;

public record Rainbow(float speed, float spread, float saturation) implements TextEffect {

    @Override
    public void apply(GlyphState g) {
        float hue = g.time * speed + g.index * spread;
        hue -= (float) Math.floor(hue);
        g.color = Gradient.keepAlpha(g.color, hsb(hue, saturation, 1f));
    }

    static int hsb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r;
        float gr;
        float b;
        int sector = (int) (h * 6) % 6;
        switch (sector) {
            case 0 -> { r = c; gr = x; b = 0; }
            case 1 -> { r = x; gr = c; b = 0; }
            case 2 -> { r = 0; gr = c; b = x; }
            case 3 -> { r = 0; gr = x; b = c; }
            case 4 -> { r = x; gr = 0; b = c; }
            default -> { r = c; gr = 0; b = x; }
        }
        return (Math.round((r + m) * 255) << 16) | (Math.round((gr + m) * 255) << 8) | Math.round((b + m) * 255);
    }
}

package dev.henny.vui.text;

public interface TextEffect {
    void apply(GlyphState glyph);

    default boolean animated() {
        return true;
    }
}

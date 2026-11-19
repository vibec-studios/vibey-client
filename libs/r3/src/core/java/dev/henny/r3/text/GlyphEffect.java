package dev.henny.r3.text;

@FunctionalInterface
public interface GlyphEffect {
    void apply(GlyphState glyph, float time);
}

package dev.henny.vui.layout;

@FunctionalInterface
public interface GlyphWidths {
    float width(int index, int codepoint);
}

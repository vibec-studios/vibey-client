package dev.henny.vui.headless;

import dev.henny.vui.render.TextMetrics;

public final class PixelFontMetrics implements TextMetrics {
    @Override
    public float glyphWidth(int codepoint, boolean bold) {
        return PixelFont.ADVANCE + (bold ? 1 : 0);
    }

    @Override
    public float lineHeight() {
        return PixelFont.LINE_HEIGHT;
    }
}

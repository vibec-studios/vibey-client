package dev.henny.vui.text;

import dev.henny.vui.anim.Interpolate;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.render.TextMetrics;

public final class GlyphRenderer {
    private final GlyphState glyph = new GlyphState();

    public float draw(Canvas canvas, TextMetrics metrics, CharSequence text, int start, int end, float x, float y,
                      int color, boolean shadow, boolean bold, TextEffect effect, float time, float letterSpacing) {
        if (effect == null && letterSpacing == 0f) {
            canvas.text(text, start, end, x, y, color, shadow, bold);
            return metrics.width(text, start, end, bold);
        }
        int count = Character.codePointCount(text, start, end);
        float total = metrics.width(text, start, end, bold) + letterSpacing * Math.max(0, count - 1);
        float pen = 0f;
        int index = 0;
        int i = start;
        while (i < end) {
            int cp = Character.codePointAt(text, i);
            float w = metrics.glyphWidth(cp, bold);
            glyph.begin(index, count, cp, time, pen, w, total, color);
            if (effect != null) effect.apply(glyph);
            if (glyph.visible && glyph.alpha > 0.004f && !Character.isWhitespace(cp)) {
                int c = glyph.alpha >= 1f ? glyph.color : Interpolate.withAlpha(glyph.color, glyph.alpha);
                canvas.glyph(cp, x + pen + glyph.dx, y + glyph.dy, c, shadow, bold);
            }
            pen += w + letterSpacing;
            index++;
            i += Character.charCount(cp);
        }
        return total;
    }

    public GlyphState lastGlyph() {
        return glyph;
    }
}

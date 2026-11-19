package dev.henny.vibeyclient.ui.hud;

import dev.henny.vibeyclient.ui.Icons;

/**
 * A widget of one or more "label value" lines ("FPS 144", "XYZ 12 64 -30"). Implement {@link #lines} (called at the
 * widget's {@link #rate()}, allocation there is fine) and call {@link #line} once per line; labels use the accent
 * colour, values the text colour. Width grows at once and shrinks only after it stayed smaller for a moment, so a
 * right-anchored FPS counter does not jitter.
 *
 * <pre>{@code
 * public final class SprintIndicator extends TextWidget {
 *     @Override protected void lines(HudPaint p) {
 *         if (p.preview || sprinting) line(null, sprintingText);
 *     }
 * }
 * }</pre>
 */
public abstract class TextWidget extends HudWidget {
    public static final int PAD_X = 4;
    public static final int PAD_Y = 3;
    public static final int LINE = 10;
    private static final int MAX = 8;

    private final String[] labels = new String[MAX];
    private final String[] values = new String[MAX];
    private final int[] labelW = new int[MAX];
    private final int[] lineW = new int[MAX];
    private int count;
    private int building;
    private int contentW;
    private long shrinkSince;

    /** Fills the lines: call {@link #line(String, String)} for each (up to 8). */
    protected abstract void lines(HudPaint p);

    /** An {@link Icons} name drawn before the first line in the accent colour (null = none). */
    protected String icon(HudPaint p) {
        return null;
    }

    /** Adds a line; {@code label} may be null (value only). */
    protected final void line(String label, String value) {
        if (building >= MAX) return;
        labels[building] = label == null || label.isEmpty() ? null : label;
        values[building] = value == null ? "" : value;
        building++;
    }

    /** Number of lines of the last update. */
    protected final int lineCount() {
        return count;
    }

    @Override
    protected final void update(HudPaint p) {
        building = 0;
        lines(p);
        count = building;
        int iconW = icon(p) != null ? Icons.SIZE + 3 : 0;
        int max = 0;
        for (int i = 0; i < count; i++) {
            labelW[i] = labels[i] == null ? 0 : p.g.width(labels[i]) + 4;
            lineW[i] = labelW[i] + p.g.width(values[i]);
            max = Math.max(max, lineW[i]);
        }
        int want = max + iconW;
        // grow now, shrink after 1.5 s of being smaller (numbers changing width must not make the box jitter)
        if (want >= contentW || contentW == 0) {
            contentW = want;
            shrinkSince = 0;
        } else if (shrinkSince == 0) {
            shrinkSince = p.now;
        } else if (p.now - shrinkSince > 1500) {
            contentW = want;
            shrinkSince = 0;
        }
        width = contentW + PAD_X * 2;
        height = Math.max(1, count) * LINE - 1 + PAD_Y * 2;
    }

    @Override
    protected boolean shown(HudPaint p) {
        return count > 0;
    }

    @Override
    protected void paint(HudPaint p) {
        if (count == 0) return;
        p.background();
        String icon = icon(p);
        int iconW = icon != null ? Icons.SIZE + 3 : 0;
        int top = (p.h - (count * LINE - 1)) / 2;
        int a = p.align();
        for (int i = 0; i < count; i++) {
            int lw = lineW[i] + (i == 0 ? iconW : 0);
            int x = a < 0 ? PAD_X : a > 0 ? p.w - PAD_X - lw : (p.w - lw) / 2;
            int y = top + i * LINE;
            if (i == 0 && icon != null) {
                Icons.draw(p.g, icon, x, y - 1, p.style.paint == HudStyle.TextPaint.RAINBOW
                        ? HudPaint.rainbowColor((p.now % 4000L) / 4000f, 0xFF) : p.style.accent);
                x += iconW;
            }
            if (labels[i] != null) {
                p.accentText(labels[i], x, y);
                x += labelW[i];
            }
            p.text(values[i], x, y);
        }
    }
}

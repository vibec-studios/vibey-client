package dev.henny.vibeyclient.ui;

import net.minecraft.client.gui.components.EditBox;

import java.util.function.Consumer;

/**
 * The basic controls in the Vibe look (square, 1 px lines, 150 ms eased hovers). Immediate mode: call them every
 * frame from a {@link Panel}; they return what happened. {@code key} objects identify per-widget animation state and
 * must be stable between frames (the setting, the module, a constant, never a new String).
 */
public final class Widgets {
    private Widgets() {}

    public enum Style { PRIMARY, SECONDARY, GHOST, DANGER, DISABLED }

    // ============================================================================================ button

    /** Flat square button with a centered label. True when clicked. */
    public static boolean button(Ui ui, Object key, int x, int y, int w, int h, String label, Style style) {
        Gfx g = ui.g;
        boolean enabled = style != Style.DISABLED;
        boolean clicked = enabled && ui.clicked(x, y, w, h);
        float hov = ui.anim(key, 1, 0f).to(enabled && ui.hover(x, y, w, h) ? 1f : 0f);
        int bg, fg, line;
        switch (style) {
            case PRIMARY -> {
                bg = Theme.mix(Theme.BRAND, Theme.BRAND_HOVER, hov);
                fg = 0xFFFFFFFF;
                line = 0;
            }
            case DANGER -> {
                bg = Theme.mix(Theme.SURFACE2, 0xFF2A1218, hov);
                fg = Theme.mix(Theme.TEXT, Theme.DANGER, hov);
                line = Theme.mix(Theme.LINE2, Theme.DANGER, hov);
            }
            case GHOST -> {
                bg = Theme.alpha(Theme.SURFACE3, hov);
                fg = Theme.mix(Theme.MUTED, Theme.TEXT, hov);
                line = 0;
            }
            case DISABLED -> {
                bg = Theme.SURFACE;
                fg = Theme.DIM;
                line = Theme.LINE;
            }
            default -> {
                bg = Theme.mix(Theme.SURFACE2, Theme.SURFACE3, hov);
                fg = Theme.TEXT;
                line = Theme.mix(Theme.LINE2, 0x40FFFFFF, hov);
            }
        }
        g.rect(x, y, w, h, bg);
        if (line != 0) g.outline(x, y, w, h, line);
        g.textCenter(g.clip(label, w - 8), x + w / 2, y + (h - 8) / 2, fg);
        return clicked;
    }

    // ============================================================================================ toggle

    public static final int TOGGLE_W = 22;
    public static final int TOGGLE_H = 12;

    /** On/off switch, top left at x, y. True when clicked (the caller flips the value). */
    public static boolean toggle(Ui ui, Object key, int x, int y, boolean on, boolean enabled) {
        Gfx g = ui.g;
        boolean clicked = enabled && ui.clicked(x - 3, y - 3, TOGGLE_W + 6, TOGGLE_H + 6);
        float t = ui.anim(key, 2, on ? 1f : 0f).to(on ? 1f : 0f);
        boolean hov = enabled && ui.hover(x - 3, y - 3, TOGGLE_W + 6, TOGGLE_H + 6);
        int track = enabled ? Theme.mix(hov ? Theme.SURFACE3 : Theme.SURFACE2, hov ? Theme.BRAND_HOVER : Theme.BRAND, t) : Theme.SURFACE2;
        g.rect(x, y, TOGGLE_W, TOGGLE_H, track);
        g.outline(x, y, TOGGLE_W, TOGGLE_H, enabled ? Theme.mix(Theme.LINE2, 0x00FFFFFF, t) : Theme.LINE);
        int knob = TOGGLE_H - 6;
        int kx = x + 3 + Math.round(t * (TOGGLE_W - knob - 6));
        g.rect(kx, y + 3, knob, knob, enabled ? Theme.mix(Theme.MUTED, 0xFFFFFFFF, t) : Theme.DIM);
        return clicked;
    }

    // ============================================================================================ slider

    /** Horizontal slider for a value in 0..1. Returns the new value (changed while dragged). */
    public static float slider(Ui ui, Object key, int x, int y, int w, float value) {
        Gfx g = ui.g;
        int h = 12;
        boolean drag = ui.dragging(key, x - 2, y - 2, w + 4, h + 4);
        if (drag) value = Math.max(0f, Math.min(1f, (ui.mouseX - x) / (float) Math.max(1, w)));
        float hov = ui.anim(key, 3, 0f).to(drag || ui.hover(x - 2, y - 2, w + 4, h + 4) ? 1f : 0f);
        int cy = y + h / 2;
        g.rect(x, cy - 1, w, 2, Theme.SURFACE3);
        int fill = Math.round(w * value);
        g.rect(x, cy - 1, fill, 2, Theme.mix(Theme.BRAND, Theme.BRAND_HOVER, hov));
        int kw = 4;
        int kx = Math.max(x, Math.min(x + w - kw, x + fill - kw / 2));
        g.rect(kx, y + 1, kw, h - 2, Theme.mix(Theme.TEXT, 0xFFFFFFFF, hov));
        return value;
    }

    // ============================================================================================ cycler

    /** Value with ‹ › arrows. Returns -1 / +1 when an arrow was clicked (a click on the value also counts +1), else 0. */
    public static int cycler(Ui ui, Object key, int x, int y, int w, String value) {
        Gfx g = ui.g;
        int h = 16;
        int result = 0;
        if (ui.clicked(x, y, 16, h)) result = -1;
        else if (ui.clicked(x + w - 16, y, 16, h)) result = 1;
        else if (ui.clicked(x + 16, y, w - 32, h)) result = 1;
        boolean hp = ui.hover(x, y, 16, h);
        boolean hn = ui.hover(x + w - 16, y, 16, h);
        g.rect(x, y, w, h, Theme.SURFACE2);
        g.outline(x, y, w, h, Theme.LINE2);
        if (hp) g.rect(x + 1, y + 1, 15, h - 2, Theme.SURFACE3);
        if (hn) g.rect(x + w - 16, y + 1, 15, h - 2, Theme.SURFACE3);
        arrow(g, x + 6, y + 5, false, hp ? Theme.TEXT : Theme.MUTED);
        arrow(g, x + w - 9, y + 5, true, hn ? Theme.TEXT : Theme.MUTED);
        g.textCenter(g.clip(value, w - 36), x + w / 2, y + 4, Theme.TEXT);
        return result;
    }

    // ============================================================================================ text field

    /**
     * Single-line text input (a vanilla EditBox in a flat box), {@code key} identifies it between frames.
     * {@code onChange} gets every edit; returns the field so callers can read focus etc.
     */
    public static EditBox textField(Ui ui, Object key, int x, int y, int w, String value, int maxLength, Consumer<String> onChange) {
        Gfx g = ui.g;
        int h = 16;
        EditBox box = ui.input(key, value, maxLength, onChange);
        boolean focused = box.isFocused();
        float f = ui.anim(box, 4, 0f).to(focused ? 1f : 0f);
        g.rect(x, y, w, h, Theme.mix(Theme.SURFACE2, Theme.SURFACE, f));
        g.outline(x, y, w, h, Theme.mix(Theme.LINE2, Theme.BRAND, f));
        box.setX(x + 5);
        box.setY(y + 4);
        box.setWidth(w - 10);
        box.visible = ui.visibleY(y, h);
        if (box.visible) g.widget(box, ui.mouseX, ui.mouseY, ui.delta);
        return box;
    }

    // ============================================================================================ swatches

    /** Row of color squares; returns the clicked color or 0. */
    public static int swatches(Ui ui, int x, int y, int[] colors, int current) {
        Gfx g = ui.g;
        int s = 12;
        int gap = 4;
        int picked = 0;
        for (int c : colors) {
            boolean sel = (c & 0xFFFFFF) == (current & 0xFFFFFF);
            if (ui.clicked(x - 1, y - 1, s + 2, s + 2)) picked = c | 0xFF000000;
            boolean hov = ui.hover(x - 1, y - 1, s + 2, s + 2);
            if (sel || hov) g.outline(x - 2, y - 2, s + 4, s + 4, sel ? Theme.TEXT : Theme.MUTED);
            g.rect(x, y, s, s, c | 0xFF000000);
            x += s + gap;
        }
        return picked;
    }

    public static int swatchesWidth(int count) {
        return count * 16 - 4;
    }

    // ============================================================================================ bits

    /** Small uppercase section caption. */
    public static void caption(Gfx g, String text, int x, int y) {
        g.text(text.toUpperCase(java.util.Locale.ROOT), x, y, Theme.DIM);
    }

    /** Colored status chip, returns its width. */
    public static int chip(Gfx g, String text, int x, int y, int color) {
        int w = g.width(text) + 8;
        g.rect(x, y, w, 12, Theme.alpha(color, 0.14f));
        g.outline(x, y, w, 12, Theme.alpha(color, 0.5f));
        g.text(text, x + 4, y + 2, color);
        return w;
    }

    /** Square icon tile with a letter. */
    public static void monogram(Gfx g, String name, int x, int y, int size, int color) {
        g.rect(x, y, size, size, Theme.alpha(color, 0.18f));
        g.outline(x, y, size, size, Theme.alpha(color, 0.6f));
        String l = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        g.textCenter(l, x + size / 2 + 1, y + (size - 8) / 2 + 1, color);
    }

    public static void cross(Gfx g, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            g.rect(x + i, y + i, 1, 1, color);
            g.rect(x + size - 1 - i, y + i, 1, 1, color);
        }
    }

    public static void arrow(Gfx g, int x, int y, boolean right, int color) {
        for (int i = 0; i < 3; i++) {
            int len = 1 + i * 2;
            int col = right ? x + 2 - i : x + i;
            g.rect(col, y + 3 - i, 1, len, color);
        }
    }
}

package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.ui.Theme;

import java.util.Locale;

/**
 * The look of a styled HUD widget. There is one global style ({@link HudLayout#globalStyle()}) and every widget may
 * override single fields of it: a {@code null} field means "inherit". {@link #resolve} merges a widget's overrides
 * onto the global style into a {@link Resolved} (plain values, cached by the layout, safe to read every frame).
 * Colours are RGB (no alpha; the background opacity is its own field).
 */
public final class HudStyle {
    public enum Corners { SQUARE, BEVEL }

    /** How the main text is coloured: one colour, the accent colour, or an animated rainbow. */
    public enum TextPaint { COLOR, ACCENT, RAINBOW }

    /** AUTO follows the anchor (left-anchored widgets align left, right-anchored right, centred centre). */
    public enum Align { AUTO, LEFT, CENTER, RIGHT }

    /** How much a widget shows (each widget decides what "detailed" adds). */
    public enum Variant { COMPACT, DETAILED }

    public Boolean background;
    public Integer bgColor;
    public Float bgOpacity;
    public Corners corners;
    public Boolean border;
    public TextPaint paint;
    public Integer textColor;
    public Integer accentColor;
    public Boolean shadow;
    public Align align;
    public Variant variant;

    public HudStyle() {}

    /** The built-in global defaults (every field set). */
    public static HudStyle defaults() {
        HudStyle s = new HudStyle();
        s.background = true;
        s.bgColor = 0x0B0B0E;
        s.bgOpacity = 0.62f;
        s.corners = Corners.SQUARE;
        s.border = false;
        s.paint = TextPaint.COLOR;
        s.textColor = 0xF4F4F5;
        s.accentColor = Theme.BRAND & 0xFFFFFF;
        s.shadow = true;
        s.align = Align.AUTO;
        s.variant = Variant.COMPACT;
        return s;
    }

    public HudStyle copy() {
        HudStyle s = new HudStyle();
        s.set(this);
        return s;
    }

    public void set(HudStyle o) {
        background = o.background;
        bgColor = o.bgColor;
        bgOpacity = o.bgOpacity;
        corners = o.corners;
        border = o.border;
        paint = o.paint;
        textColor = o.textColor;
        accentColor = o.accentColor;
        shadow = o.shadow;
        align = o.align;
        variant = o.variant;
    }

    /** Fields set in {@code o} replace the ones here. */
    public void overlay(HudStyle o) {
        if (o == null) return;
        if (o.background != null) background = o.background;
        if (o.bgColor != null) bgColor = o.bgColor;
        if (o.bgOpacity != null) bgOpacity = o.bgOpacity;
        if (o.corners != null) corners = o.corners;
        if (o.border != null) border = o.border;
        if (o.paint != null) paint = o.paint;
        if (o.textColor != null) textColor = o.textColor;
        if (o.accentColor != null) accentColor = o.accentColor;
        if (o.shadow != null) shadow = o.shadow;
        if (o.align != null) align = o.align;
        if (o.variant != null) variant = o.variant;
    }

    /** No field overridden. */
    public boolean isEmpty() {
        return background == null && bgColor == null && bgOpacity == null && corners == null && border == null
                && paint == null && textColor == null && accentColor == null && shadow == null && align == null
                && variant == null;
    }

    public void clear() {
        set(new HudStyle());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HudStyle o)) return false;
        return java.util.Objects.equals(background, o.background) && java.util.Objects.equals(bgColor, o.bgColor)
                && java.util.Objects.equals(bgOpacity, o.bgOpacity) && corners == o.corners
                && java.util.Objects.equals(border, o.border) && paint == o.paint
                && java.util.Objects.equals(textColor, o.textColor) && java.util.Objects.equals(accentColor, o.accentColor)
                && java.util.Objects.equals(shadow, o.shadow) && align == o.align && variant == o.variant;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(background, bgColor, bgOpacity, corners, border, paint, textColor, accentColor, shadow, align, variant);
    }

    // ============================================================================================ resolved

    /** A fully resolved style (no nulls). Mutable for reuse; owned by the layout, do not keep references. */
    public static final class Resolved {
        public boolean background;
        /** ARGB, opacity applied. */
        public int bg;
        public Corners corners = Corners.SQUARE;
        public boolean border;
        public TextPaint paint = TextPaint.COLOR;
        /** ARGB, opaque. */
        public int text;
        public int accent;
        public boolean shadow;
        public Align align = Align.AUTO;
        public Variant variant = Variant.COMPACT;

        public boolean detailed() {
            return variant == Variant.DETAILED;
        }
    }

    /** Merges {@code own} onto {@code global} into {@code out}. */
    public static Resolved resolve(HudStyle global, HudStyle own, Resolved out) {
        HudStyle d = DEFAULTS;
        boolean bgOn = pick(own == null ? null : own.background, global.background, d.background);
        int bgRgb = pick(own == null ? null : own.bgColor, global.bgColor, d.bgColor);
        float op = pick(own == null ? null : own.bgOpacity, global.bgOpacity, d.bgOpacity);
        out.background = bgOn;
        out.bg = (Math.round(Math.max(0f, Math.min(1f, op)) * 255f) << 24) | (bgRgb & 0xFFFFFF);
        out.corners = pick(own == null ? null : own.corners, global.corners, d.corners);
        out.border = pick(own == null ? null : own.border, global.border, d.border);
        out.paint = pick(own == null ? null : own.paint, global.paint, d.paint);
        out.text = 0xFF000000 | (pick(own == null ? null : own.textColor, global.textColor, d.textColor) & 0xFFFFFF);
        out.accent = 0xFF000000 | (pick(own == null ? null : own.accentColor, global.accentColor, d.accentColor) & 0xFFFFFF);
        out.shadow = pick(own == null ? null : own.shadow, global.shadow, d.shadow);
        out.align = pick(own == null ? null : own.align, global.align, d.align);
        out.variant = pick(own == null ? null : own.variant, global.variant, d.variant);
        return out;
    }

    private static final HudStyle DEFAULTS = defaults();

    private static <T> T pick(T own, T global, T def) {
        return own != null ? own : global != null ? global : def;
    }

    // ============================================================================================ json

    /** Compact JSON (only the set fields; short keys, used in hud-layout.json and share codes). */
    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        if (background != null) o.addProperty("bg", background);
        if (bgColor != null) o.addProperty("bc", hex(bgColor));
        if (bgOpacity != null) o.addProperty("bo", Math.round(bgOpacity * 100f) / 100f);
        if (corners != null) o.addProperty("co", corners.name().toLowerCase(Locale.ROOT));
        if (border != null) o.addProperty("bd", border);
        if (paint != null) o.addProperty("tp", paint.name().toLowerCase(Locale.ROOT));
        if (textColor != null) o.addProperty("tc", hex(textColor));
        if (accentColor != null) o.addProperty("ac", hex(accentColor));
        if (shadow != null) o.addProperty("sh", shadow);
        if (align != null) o.addProperty("al", align.name().toLowerCase(Locale.ROOT));
        if (variant != null) o.addProperty("va", variant.name().toLowerCase(Locale.ROOT));
        return o;
    }

    /** Reads what it understands; bad fields are skipped (never throws). */
    public static HudStyle fromJson(JsonElement el) {
        HudStyle s = new HudStyle();
        if (el == null || !el.isJsonObject()) return s;
        JsonObject o = el.getAsJsonObject();
        s.background = bool(o, "bg");
        s.bgColor = color(o, "bc");
        Float bo = num(o, "bo");
        s.bgOpacity = bo == null ? null : Math.max(0f, Math.min(1f, bo));
        s.corners = enumOf(Corners.class, o, "co");
        s.border = bool(o, "bd");
        s.paint = enumOf(TextPaint.class, o, "tp");
        s.textColor = color(o, "tc");
        s.accentColor = color(o, "ac");
        s.shadow = bool(o, "sh");
        s.align = enumOf(Align.class, o, "al");
        s.variant = enumOf(Variant.class, o, "va");
        return s;
    }

    static String hex(int rgb) {
        return String.format(Locale.ROOT, "%06x", rgb & 0xFFFFFF);
    }

    private static Boolean bool(JsonObject o, String k) {
        try {
            JsonElement e = o.get(k);
            return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Float num(JsonObject o, String k) {
        try {
            JsonElement e = o.get(k);
            if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
            float f = e.getAsFloat();
            return Float.isFinite(f) ? f : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Integer color(JsonObject o, String k) {
        try {
            JsonElement e = o.get(k);
            if (e == null || !e.isJsonPrimitive()) return null;
            String s = e.getAsString().trim();
            if (s.startsWith("#")) s = s.substring(1);
            if (!s.matches("[0-9a-fA-F]{6}")) return null;
            return Integer.parseInt(s, 16);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, JsonObject o, String k) {
        try {
            JsonElement e = o.get(k);
            if (e == null || !e.isJsonPrimitive()) return null;
            return Enum.valueOf(type, e.getAsString().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return null;
        }
    }
}

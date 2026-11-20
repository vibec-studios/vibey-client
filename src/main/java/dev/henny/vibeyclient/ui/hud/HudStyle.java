package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.ui.Theme;

import java.util.Objects;

public final class HudStyle {
    public enum Corners { SQUARE, BEVEL }

    public enum TextPaint { COLOR, ACCENT, RAINBOW }

    public enum Align { AUTO, LEFT, CENTER, RIGHT }

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

    public boolean isEmpty() {
        return equals(EMPTY);
    }

    public void clear() {
        set(EMPTY);
    }

    public JsonObject toJson() {
        return HudStyleJson.write(this);
    }

    public static HudStyle fromJson(JsonElement el) {
        return HudStyleJson.read(el);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HudStyle o)) return false;
        return Objects.equals(background, o.background) && Objects.equals(bgColor, o.bgColor)
                && Objects.equals(bgOpacity, o.bgOpacity) && corners == o.corners && Objects.equals(border, o.border)
                && paint == o.paint && Objects.equals(textColor, o.textColor) && Objects.equals(accentColor, o.accentColor)
                && Objects.equals(shadow, o.shadow) && align == o.align && variant == o.variant;
    }

    @Override
    public int hashCode() {
        return Objects.hash(background, bgColor, bgOpacity, corners, border, paint, textColor, accentColor, shadow, align, variant);
    }

    public static final class Resolved {
        public boolean background;
        public int bg;
        public Corners corners = Corners.SQUARE;
        public boolean border;
        public TextPaint paint = TextPaint.COLOR;
        public int text;
        public int accent;
        public boolean shadow;
        public Align align = Align.AUTO;
        public Variant variant = Variant.COMPACT;

        public boolean detailed() {
            return variant == Variant.DETAILED;
        }
    }

    private static final HudStyle EMPTY = new HudStyle();
    private static final HudStyle DEFAULTS = defaults();

    public static Resolved resolve(HudStyle global, HudStyle own, Resolved out) {
        HudStyle o = own == null ? EMPTY : own;
        HudStyle d = DEFAULTS;
        float op = pick(o.bgOpacity, global.bgOpacity, d.bgOpacity);
        out.background = pick(o.background, global.background, d.background);
        out.bg = (Math.round(Math.max(0f, Math.min(1f, op)) * 255f) << 24) | (pick(o.bgColor, global.bgColor, d.bgColor) & 0xFFFFFF);
        out.corners = pick(o.corners, global.corners, d.corners);
        out.border = pick(o.border, global.border, d.border);
        out.paint = pick(o.paint, global.paint, d.paint);
        out.text = 0xFF000000 | (pick(o.textColor, global.textColor, d.textColor) & 0xFFFFFF);
        out.accent = 0xFF000000 | (pick(o.accentColor, global.accentColor, d.accentColor) & 0xFFFFFF);
        out.shadow = pick(o.shadow, global.shadow, d.shadow);
        out.align = pick(o.align, global.align, d.align);
        out.variant = pick(o.variant, global.variant, d.variant);
        return out;
    }

    private static <T> T pick(T own, T global, T def) {
        return own != null ? own : global != null ? global : def;
    }
}

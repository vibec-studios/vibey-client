package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.ui.hud.HudStyle;
import dev.henny.vui.bind.Model;

import java.util.Locale;

final class StyleValues {
    private StyleValues() {}

    static void fill(Model st, HudStyle.Resolved r, HudStyle own) {
        st.value("bg", r.background).value("bgColor", hex(r.bg)).value("opacity", Math.round(((r.bg >>> 24) & 0xFF) / 2.55f) / 100f);
        st.value("corners", lower(r.corners)).value("border", r.border).value("paint", lower(r.paint));
        st.value("textColor", hex(r.text)).value("accent", hex(r.accent)).value("shadow", r.shadow);
        st.value("align", lower(r.align)).value("variant", lower(r.variant));
        boolean o = own != null;
        st.value("o_bg", o && own.background != null).value("o_bgColor", o && own.bgColor != null);
        st.value("o_opacity", o && own.bgOpacity != null).value("o_corners", o && own.corners != null);
        st.value("o_border", o && own.border != null).value("o_paint", o && own.paint != null);
        st.value("o_textColor", o && own.textColor != null).value("o_accent", o && own.accentColor != null);
        st.value("o_shadow", o && own.shadow != null).value("o_align", o && own.align != null);
        st.value("o_variant", o && own.variant != null);
    }

    static void write(HudStyle s, String key, Object v) {
        switch (key) {
            case "bg" -> s.background = v == null ? null : truthy(v);
            case "bgColor" -> s.bgColor = color(v);
            case "opacity" -> s.bgOpacity = v == null ? null : Math.round(number(v) * 100f) / 100f;
            case "corners" -> s.corners = v == null ? null : HudStyle.Corners.valueOf(upper(v));
            case "border" -> s.border = v == null ? null : truthy(v);
            case "paint" -> s.paint = v == null ? null : HudStyle.TextPaint.valueOf(upper(v));
            case "textColor" -> {
                s.textColor = color(v);
                if (v != null) s.paint = HudStyle.TextPaint.COLOR;
            }
            case "accent" -> s.accentColor = color(v);
            case "shadow" -> s.shadow = v == null ? null : truthy(v);
            case "align" -> s.align = v == null ? null : HudStyle.Align.valueOf(upper(v));
            case "variant" -> s.variant = v == null ? null : HudStyle.Variant.valueOf(upper(v));
            default -> {
            }
        }
    }

    static String hex(int argb) {
        return String.format(Locale.ROOT, "#%06x", argb & 0xFFFFFF);
    }

    private static Integer color(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue() & 0xFFFFFF;
        String s = v.toString().trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 8) s = s.substring(2);
        return s.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(s, 16) : null;
    }

    private static boolean truthy(Object v) {
        return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString());
    }

    private static float number(Object v) {
        float f = v instanceof Number n ? n.floatValue() : Float.parseFloat(v.toString());
        return Math.max(0f, Math.min(1f, f));
    }

    private static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }

    private static String upper(Object v) {
        return v.toString().toUpperCase(Locale.ROOT);
    }
}

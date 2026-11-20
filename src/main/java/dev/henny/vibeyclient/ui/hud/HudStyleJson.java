package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

final class HudStyleJson {
    private HudStyleJson() {}

    static JsonObject write(HudStyle s) {
        JsonObject o = new JsonObject();
        if (s.background != null) o.addProperty("bg", s.background);
        if (s.bgColor != null) o.addProperty("bc", hex(s.bgColor));
        if (s.bgOpacity != null) o.addProperty("bo", Math.round(s.bgOpacity * 100f) / 100f);
        if (s.corners != null) o.addProperty("co", lower(s.corners));
        if (s.border != null) o.addProperty("bd", s.border);
        if (s.paint != null) o.addProperty("tp", lower(s.paint));
        if (s.textColor != null) o.addProperty("tc", hex(s.textColor));
        if (s.accentColor != null) o.addProperty("ac", hex(s.accentColor));
        if (s.shadow != null) o.addProperty("sh", s.shadow);
        if (s.align != null) o.addProperty("al", lower(s.align));
        if (s.variant != null) o.addProperty("va", lower(s.variant));
        return o;
    }

    static HudStyle read(JsonElement el) {
        HudStyle s = new HudStyle();
        if (el == null || !el.isJsonObject()) return s;
        JsonObject o = el.getAsJsonObject();
        s.background = JsonRead.boolOrNull(o, "bg");
        s.bgColor = color(o, "bc");
        Float bo = JsonRead.floatOrNull(o, "bo");
        s.bgOpacity = bo == null ? null : Math.max(0f, Math.min(1f, bo));
        s.corners = JsonRead.enumOrNull(HudStyle.Corners.class, o, "co");
        s.border = JsonRead.boolOrNull(o, "bd");
        s.paint = JsonRead.enumOrNull(HudStyle.TextPaint.class, o, "tp");
        s.textColor = color(o, "tc");
        s.accentColor = color(o, "ac");
        s.shadow = JsonRead.boolOrNull(o, "sh");
        s.align = JsonRead.enumOrNull(HudStyle.Align.class, o, "al");
        s.variant = JsonRead.enumOrNull(HudStyle.Variant.class, o, "va");
        return s;
    }

    static String hex(int rgb) {
        return String.format(Locale.ROOT, "%06x", rgb & 0xFFFFFF);
    }

    private static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }

    private static Integer color(JsonObject o, String k) {
        String s = JsonRead.str(o, k, null);
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        return s.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(s, 16) : null;
    }
}

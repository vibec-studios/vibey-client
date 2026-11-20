package dev.henny.vui.style;

import dev.henny.vui.text.TextEffects;

public final class ValueParser {
    private ValueParser() {
    }

    public static StyleValue parse(Prop prop, String raw) {
        String v = raw.trim();
        if (v.isEmpty()) throw new StyleError("empty value for " + prop.cssName(), null);
        if (v.equals("inherit")) return StyleValue.INHERIT_VALUE;
        if (v.equals("initial")) return StyleValue.INITIAL_VALUE;
        try {
            return switch (prop.kind()) {
                case LENGTH -> length(prop, v);
                case NUMBER -> StyleValue.number(number(prop, v));
                case COLOR -> StyleValue.color(ColorParser.parse(v));
                case KEYWORD -> keyword(prop, v);
                case STRING -> StyleValue.object(string(v));
                case TIME -> StyleValue.number(CssValues.time(v));
                case OBJECT -> StyleValue.object(object(prop, v));
            };
        } catch (NumberFormatException e) {
            throw new StyleError("'" + v + "' is not a valid " + prop.cssName(), hintFor(prop));
        }
    }

    public static StyleValue initial(Prop prop) {
        return parse(prop, prop.initial());
    }

    private static StyleValue length(Prop prop, String v) {
        if (v.equals("auto")) return StyleValue.length(0, Unit.AUTO);
        if (v.equals("none")) return StyleValue.length(0, Unit.NONE);
        if (v.endsWith("%")) return StyleValue.length(Float.parseFloat(v.substring(0, v.length() - 1)), Unit.PERCENT);
        String n = v.endsWith("px") ? v.substring(0, v.length() - 2) : v;
        try {
            return StyleValue.length(Float.parseFloat(n), Unit.PX);
        } catch (NumberFormatException e) {
            throw new StyleError("'" + v + "' is not a length for " + prop.cssName(), "use a number (GUI px), 50%, auto or none");
        }
    }

    private static float number(Prop prop, String v) {
        if (v.endsWith("deg")) return Float.parseFloat(v.substring(0, v.length() - 3));
        if (v.endsWith("%")) return Float.parseFloat(v.substring(0, v.length() - 1)) / 100f;
        if (v.equals("infinite")) return Float.POSITIVE_INFINITY;
        if (prop == Prop.ASPECT_RATIO) return ratio(v);
        try {
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            throw new StyleError("'" + v + "' is not a number for " + prop.cssName(), hintFor(prop));
        }
    }

    private static float ratio(String v) {
        if (v.equals("auto") || v.equals("none")) return 0f;
        int slash = v.indexOf('/');
        if (slash < 0) return Float.parseFloat(v);
        float w = Float.parseFloat(v.substring(0, slash).trim());
        float h = Float.parseFloat(v.substring(slash + 1).trim());
        return h == 0 ? 0f : w / h;
    }

    private static StyleValue keyword(Prop prop, String v) {
        String k = normalizeKeyword(prop, v);
        if (!prop.allows(k)) {
            throw new StyleError("'" + v + "' is not allowed for " + prop.cssName(), "use one of: " + String.join(", ", prop.keywords()));
        }
        return StyleValue.object(k.intern());
    }

    private static String normalizeKeyword(Prop prop, String v) {
        String k = v.toLowerCase();
        if (k.equals("flex-start") || k.equals("left") && prop != Prop.ANCHOR || k.equals("top") && prop != Prop.ANCHOR) return "start";
        if (k.equals("flex-end") || k.equals("right") && prop != Prop.ANCHOR || k.equals("bottom") && prop != Prop.ANCHOR) return "end";
        if (prop == Prop.TEXT_SHADOW && (k.equals("true") || k.equals("yes"))) return "on";
        if (prop == Prop.TEXT_SHADOW && k.equals("false")) return "none";
        if (prop == Prop.FONT_WEIGHT && (k.equals("700") || k.equals("bolder"))) return "bold";
        if (prop == Prop.TEXT_WRAP && k.equals("normal")) return "wrap";
        if (prop == Prop.DISPLAY && k.equals("flex")) return "row";
        if (prop == Prop.DISPLAY && k.equals("block")) return "column";
        if (prop == Prop.ANCHOR && k.equals("middle")) return "center";
        return k;
    }

    private static String string(String v) {
        if (v.equals("none")) return "";
        if (v.startsWith("url(") && v.endsWith(")")) v = v.substring(4, v.length() - 1).trim();
        if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\"") || v.startsWith("'") && v.endsWith("'"))) {
            v = v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static Object object(Prop prop, String v) {
        return switch (prop) {
            case TRANSITION -> TransitionSpec.parseList(v);
            case ANIMATION -> AnimationSpec.parseList(v);
            case TEXT_EFFECT -> TextEffects.parse(v);
            default -> v;
        };
    }

    private static String hintFor(Prop prop) {
        return switch (prop.kind()) {
            case LENGTH -> "use a number (GUI px), 50%, auto or none";
            case NUMBER -> "use a plain number like 0.5";
            case COLOR -> "use #rrggbb, #rrggbbaa or a $token";
            default -> null;
        };
    }
}

package dev.henny.vui.anim;

public final class Easings {
    public static final Easing LINEAR = t -> t;
    public static final Easing OUT = new CubicBezier(0.2f, 0.8f, 0.2f, 1f);
    public static final Easing IN = new CubicBezier(0.4f, 0f, 1f, 1f);
    public static final Easing IN_OUT = new CubicBezier(0.4f, 0f, 0.2f, 1f);
    public static final Easing EASE = new CubicBezier(0.25f, 0.1f, 0.25f, 1f);
    public static final Easing BACK_OUT = new CubicBezier(0.34f, 1.56f, 0.64f, 1f);
    public static final Easing STEP_END = t -> t >= 1f ? 1f : 0f;

    public static final int DEFAULT_DURATION_MS = 150;

    private Easings() {
    }

    public static Easing named(String name) {
        return switch (name) {
            case "linear" -> LINEAR;
            case "out", "ease-out", "vibe" -> OUT;
            case "in", "ease-in" -> IN;
            case "in-out", "ease-in-out" -> IN_OUT;
            case "ease" -> EASE;
            case "back", "back-out", "overshoot" -> BACK_OUT;
            case "step-end", "steps" -> STEP_END;
            default -> null;
        };
    }

    public static Easing parse(String text) {
        String s = text.trim();
        Easing named = named(s);
        if (named != null) return named;
        if (s.startsWith("cubic-bezier(") && s.endsWith(")")) {
            String[] parts = s.substring(13, s.length() - 1).split(",");
            if (parts.length != 4) return null;
            try {
                return new CubicBezier(Float.parseFloat(parts[0].trim()), Float.parseFloat(parts[1].trim()),
                        Float.parseFloat(parts[2].trim()), Float.parseFloat(parts[3].trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static boolean isEasing(String text) {
        return parse(text) != null;
    }
}

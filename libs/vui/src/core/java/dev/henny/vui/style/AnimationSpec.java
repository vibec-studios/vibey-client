package dev.henny.vui.style;

import dev.henny.vui.anim.Easing;
import dev.henny.vui.anim.Easings;

import java.util.ArrayList;
import java.util.List;

public record AnimationSpec(String name, float duration, float delay, Easing easing, float iterations,
                            boolean alternate, boolean reverse, boolean fillForwards) {

    public boolean infinite() {
        return Float.isInfinite(iterations);
    }

    public static List<AnimationSpec> parseList(String value) {
        List<AnimationSpec> out = new ArrayList<>();
        if (value.trim().equals("none")) return out;
        for (String item : CssValues.splitTopLevel(value, ',')) out.add(parseOne(item));
        return out;
    }

    private static AnimationSpec parseOne(String item) {
        String name = null;
        float duration = 1f;
        float delay = 0f;
        boolean sawDuration = false;
        Easing easing = Easings.LINEAR;
        float iterations = 1f;
        boolean alternate = false;
        boolean reverse = false;
        boolean fill = false;
        for (String word : CssValues.words(item)) {
            if (CssValues.isTime(word)) {
                if (!sawDuration) duration = CssValues.time(word);
                else delay = CssValues.time(word);
                sawDuration = true;
            } else if (word.equals("infinite")) {
                iterations = Float.POSITIVE_INFINITY;
            } else if (word.equals("alternate")) {
                alternate = true;
            } else if (word.equals("reverse")) {
                reverse = true;
            } else if (word.equals("forwards") || word.equals("both")) {
                fill = true;
            } else if (word.equals("normal") || word.equals("none")) {
                continue;
            } else if (Easings.isEasing(word)) {
                easing = Easings.parse(word);
            } else if (isNumber(word)) {
                iterations = Float.parseFloat(word);
            } else {
                name = word;
            }
        }
        if (name == null) throw new StyleError("animation needs a @keyframes name", "e.g. animation: pulse 1s infinite");
        return new AnimationSpec(name, duration, delay, easing, iterations, alternate, reverse, fill);
    }

    private static boolean isNumber(String s) {
        try {
            Float.parseFloat(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

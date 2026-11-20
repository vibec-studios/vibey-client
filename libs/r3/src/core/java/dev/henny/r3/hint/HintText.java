package dev.henny.r3.hint;

import dev.henny.r3.input.Gesture;
import dev.henny.r3.input.GestureBindings;

public final class HintText {
    private HintText() {}

    public static String format(String template, GestureBindings bindings) {
        String out = template;
        for (Gesture g : Gesture.values()) {
            String token = "{" + g.name() + "}";
            if (out.contains(token)) out = out.replace(token, bindings.displayName(g));
        }
        return out;
    }

    public static String format(Hint hint, GestureBindings bindings) {
        return hint == null ? null : format(hint.template(), bindings);
    }
}

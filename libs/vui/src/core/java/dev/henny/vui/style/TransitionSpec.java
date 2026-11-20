package dev.henny.vui.style;

import dev.henny.vui.anim.Easing;
import dev.henny.vui.anim.Easings;

import java.util.ArrayList;
import java.util.List;

public record TransitionSpec(Prop prop, boolean all, float duration, float delay, Easing easing) {

    public boolean covers(Prop p) {
        return all ? p.kind().interpolates() : prop == p;
    }

    public static List<TransitionSpec> parseList(String value) {
        List<TransitionSpec> out = new ArrayList<>();
        if (value.trim().equals("none")) return out;
        for (String item : CssValues.splitTopLevel(value, ',')) out.addAll(parseOne(item));
        return out;
    }

    private static List<TransitionSpec> parseOne(String item) {
        String target = "all";
        float duration = Easings.DEFAULT_DURATION_MS / 1000f;
        float delay = 0f;
        boolean sawDuration = false;
        Easing easing = Easings.OUT;
        for (String word : CssValues.words(item)) {
            if (CssValues.isTime(word)) {
                if (!sawDuration) duration = CssValues.time(word);
                else delay = CssValues.time(word);
                sawDuration = true;
            } else if (Easings.isEasing(word)) {
                easing = Easings.parse(word);
            } else {
                target = word;
            }
        }
        List<TransitionSpec> out = new ArrayList<>();
        if (target.equals("all")) {
            out.add(new TransitionSpec(null, true, duration, delay, easing));
            return out;
        }
        List<Prop> props = Shorthands.longhandsOf(PropertyNames.canonical(target));
        if (props.isEmpty()) throw new StyleError("cannot transition unknown property '" + target + "'", "use a property name or 'all'");
        for (Prop p : props) out.add(new TransitionSpec(p, false, duration, delay, easing));
        return out;
    }
}

package dev.henny.vui.anim;

import dev.henny.vui.diag.ErrorSink;
import dev.henny.vui.style.AnimationSpec;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Declaration;
import dev.henny.vui.style.Keyframes;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.PropKind;
import dev.henny.vui.style.StyleValue;
import dev.henny.vui.style.Theme;

import java.util.List;

final class KeyframePlayer {
    final AnimationSpec spec;
    private final Keyframes frames;
    private float elapsed;
    private boolean finished;

    KeyframePlayer(AnimationSpec spec, Keyframes frames) {
        this.spec = spec;
        this.frames = frames;
    }

    boolean finished() {
        return finished;
    }

    boolean touches(boolean layout, boolean inherited) {
        for (Keyframes.Stop stop : frames.stops()) {
            for (Declaration d : stop.declarations()) {
                for (int i = 0; i < d.size(); i++) {
                    Prop p = d.prop(i);
                    if (layout && p.affectsLayout() || inherited && p.inherited()) return true;
                }
            }
        }
        return false;
    }

    void step(float dt, ComputedStyle base, ComputedStyle out, Theme theme, ErrorSink errors) {
        if (!Animations.enabled()) {
            finished = !spec.infinite();
            return;
        }
        elapsed += dt * Animations.speed();
        float local = elapsed - spec.delay();
        if (local < 0) return;
        float duration = Math.max(0.001f, spec.duration());
        float iteration = local / duration;
        if (!spec.infinite() && iteration >= spec.iterations()) {
            finished = true;
            if (spec.fillForwards()) apply(endProgress(), base, out, theme, errors);
            return;
        }
        int index = (int) Math.floor(iteration);
        float t = iteration - index;
        boolean backwards = spec.reverse() ^ (spec.alternate() && (index & 1) == 1);
        apply(backwards ? 1f - t : t, base, out, theme, errors);
    }

    private float endProgress() {
        int last = (int) Math.ceil(spec.iterations()) - 1;
        boolean backwards = spec.reverse() ^ (spec.alternate() && (last & 1) == 1);
        return backwards ? 0f : 1f;
    }

    private void apply(float progress, ComputedStyle base, ComputedStyle out, Theme theme, ErrorSink errors) {
        List<Keyframes.Stop> stops = frames.stops();
        if (stops.isEmpty()) return;
        float eased = spec.easing().apply(progress);
        for (int s = 0; s < stops.size(); s++) {
            for (Declaration d : stops.get(s).declarations()) {
                if (!d.resolve(theme, errors)) continue;
                for (int i = 0; i < d.size(); i++) applyProp(d.prop(i), eased, stops, base, out, theme, errors);
            }
        }
    }

    private void applyProp(Prop p, float t, List<Keyframes.Stop> stops, ComputedStyle base, ComputedStyle out, Theme theme, ErrorSink errors) {
        float fromOffset = 0f;
        float toOffset = 1f;
        StyleValue from = null;
        StyleValue to = null;
        for (Keyframes.Stop stop : stops) {
            StyleValue v = valueIn(stop, p, theme, errors);
            if (v == null) continue;
            if (stop.offset() <= t) {
                from = v;
                fromOffset = stop.offset();
            }
            if (stop.offset() >= t && to == null) {
                to = v;
                toOffset = stop.offset();
            }
        }
        if (from == null && to == null) return;
        if (from == null) from = to;
        if (to == null) to = from;
        float span = toOffset - fromOffset;
        float local = span <= 0 ? 1f : (t - fromOffset) / span;
        if (p.kind() == PropKind.COLOR) out.setColor(p, Interpolate.color(from.color(), to.color(), local));
        else if (p.kind() == PropKind.LENGTH || p.kind() == PropKind.NUMBER) out.setNumber(p, Interpolate.lerp(from.number(), to.number(), local));
        else out.set(p, local < 0.5f ? from : to);
    }

    private static StyleValue valueIn(Keyframes.Stop stop, Prop p, Theme theme, ErrorSink errors) {
        StyleValue found = null;
        for (Declaration d : stop.declarations()) {
            if (!d.resolve(theme, errors)) continue;
            for (int i = 0; i < d.size(); i++) if (d.prop(i) == p) found = d.value(i);
        }
        return found;
    }
}

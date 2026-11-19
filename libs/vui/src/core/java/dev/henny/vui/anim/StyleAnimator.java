package dev.henny.vui.anim;

import dev.henny.vui.diag.ErrorSink;
import dev.henny.vui.dom.Element;
import dev.henny.vui.style.AnimationSpec;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Keyframes;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.StyleResolver;
import dev.henny.vui.style.TransitionSpec;

import java.util.ArrayList;
import java.util.List;

public final class StyleAnimator {
    private final Element element;
    private final List<PropTrack> tracks = new ArrayList<>();
    private final List<KeyframePlayer> players = new ArrayList<>();
    private Object playingSpecs;
    private boolean inheritedAnimating;

    public StyleAnimator(Element element) {
        this.element = element;
    }

    public void transition(Prop p, TransitionSpec spec) {
        PropTrack track = track(p);
        track.start(element.style(), element.computed(), spec.duration(), spec.delay(), spec.easing());
        if (!track.running()) element.style().copy(p, element.computed());
    }

    public void cancel(Prop p) {
        for (int i = tracks.size() - 1; i >= 0; i--) if (tracks.get(i).prop == p) tracks.remove(i);
    }

    public void animations(Object specs, StyleResolver resolver) {
        if (specs == playingSpecs || specs != null && specs.equals(playingSpecs)) return;
        playingSpecs = specs;
        players.clear();
        if (!(specs instanceof List<?> list)) return;
        for (Object o : list) {
            if (!(o instanceof AnimationSpec spec)) continue;
            Keyframes frames = resolver.keyframes(spec.name());
            if (frames == null) {
                element.document().errors().report(element.pos(), "unknown @keyframes '" + spec.name() + "'", "define @keyframes " + spec.name() + " { … } in a <style>");
                continue;
            }
            players.add(new KeyframePlayer(spec, frames));
        }
    }

    public boolean running() {
        return !tracks.isEmpty() || !players.isEmpty();
    }

    public boolean hasTrack(Prop p) {
        for (PropTrack t : tracks) if (t.prop == p) return true;
        return false;
    }

    public boolean inheritedAnimating() {
        return inheritedAnimating;
    }

    public boolean step(float dt, StyleResolver resolver, ErrorSink errors) {
        ComputedStyle used = element.style();
        inheritedAnimating = false;
        boolean layout = false;
        for (int i = tracks.size() - 1; i >= 0; i--) {
            PropTrack t = tracks.get(i);
            t.step(dt, used);
            if (t.prop.inherited()) inheritedAnimating = true;
            if (t.prop.affectsLayout()) layout = true;
            if (!t.running()) {
                used.copy(t.prop, element.computed());
                tracks.remove(i);
            }
        }
        for (int i = players.size() - 1; i >= 0; i--) {
            KeyframePlayer p = players.get(i);
            p.step(dt, element.computed(), used, resolver.theme(), errors);
            if (p.touches(true, false)) layout = true;
            if (p.touches(false, true)) inheritedAnimating = true;
            if (p.finished()) {
                players.remove(i);
                if (!p.spec.fillForwards()) used.copyFrom(element.computed());
            }
        }
        return layout;
    }

    private PropTrack track(Prop p) {
        for (PropTrack t : tracks) if (t.prop == p) return t;
        PropTrack t = new PropTrack(p);
        tracks.add(t);
        return t;
    }
}

package dev.henny.vui.anim;

import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.PropKind;

final class PropTrack {
    final Prop prop;
    final Tween tween = new Tween(0f);
    float fromNumber;
    float toNumber;
    int fromColor;
    int toColor;

    PropTrack(Prop prop) {
        this.prop = prop;
    }

    void start(ComputedStyle current, ComputedStyle target, float duration, float delay, Easing easing) {
        if (prop.kind() == PropKind.COLOR) {
            fromColor = current.color(prop);
            toColor = target.color(prop);
        } else {
            fromNumber = current.number(prop);
            toNumber = target.number(prop);
        }
        tween.start(0f, 1f, duration, delay, easing);
    }

    void step(float dt, ComputedStyle out) {
        tween.step(dt);
        write(out);
    }

    void write(ComputedStyle out) {
        float t = tween.running() ? tween.eased() : 1f;
        if (prop.kind() == PropKind.COLOR) out.setColor(prop, Interpolate.color(fromColor, toColor, t));
        else out.setNumber(prop, Interpolate.lerp(fromNumber, toNumber, t));
    }

    boolean running() {
        return tween.running();
    }
}

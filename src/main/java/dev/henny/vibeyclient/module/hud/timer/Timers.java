package dev.henny.vibeyclient.module.hud.timer;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class Timers {
    private Timers() {}

    private static final List<WeakReference<TimerWidget>> ALL = new ArrayList<>();

    static synchronized void add(TimerWidget w) {
        ALL.removeIf(r -> r.get() == null);
        ALL.add(new WeakReference<>(w));
    }

    static synchronized List<TimerWidget> live() {
        List<TimerWidget> out = new ArrayList<>();
        for (WeakReference<TimerWidget> r : ALL) {
            TimerWidget w = r.get();
            if (w != null && w.element() != null && HudLayout.get(w.element().id()) == w.element() && !HudLayout.hidden(w.element())) out.add(w);
        }
        return out;
    }

    public static void toggle() {
        long now = System.currentTimeMillis();
        for (TimerWidget w : live()) {
            w.toggle(now);
        }
    }

    public static void lap() {
        long now = System.currentTimeMillis();
        for (TimerWidget w : live()) {
            w.clock.lapOrReset(now);
            w.invalidate();
        }
    }

    public static TimerWidget at(double x, double y, int sw, int sh) {
        HudLayout.Box b = new HudLayout.Box();
        for (TimerWidget w : live()) {
            HudElement e = w.element();
            HudLayout.box(e, sw, sh, b);
            if (x >= b.x && x < b.x + b.w && y >= b.y && y < b.y + b.h) return w;
        }
        return null;
    }

    public static boolean running() {
        for (TimerWidget w : live()) if (w.clock.running()) return true;
        return false;
    }
}

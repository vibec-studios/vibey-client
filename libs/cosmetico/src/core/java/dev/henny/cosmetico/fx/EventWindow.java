package dev.henny.cosmetico.fx;

import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Event;

import java.util.function.Consumer;

public final class EventWindow {
    private EventWindow() {}

    public static void run(Animation a, float from, float to, boolean loop, Consumer<Event> fire) {
        if (from < 0f) {
            fire(a, -1f, to, fire);
        } else if (to >= from) {
            fire(a, from, to, fire);
        } else if (loop) {
            fire(a, from, a.length, fire);
            fire(a, -1f, to, fire);
        }
    }

    private static void fire(Animation a, float from, float to, Consumer<Event> fire) {
        for (Event ev : a.events) if (ev.t > from && ev.t <= to + 1e-5f) fire.accept(ev);
    }
}

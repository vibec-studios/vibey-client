package dev.henny.vui.event;

import dev.henny.vui.dom.Element;

import java.util.List;

public final class Dispatcher {
    private static final int MAX_DEPTH = 8;

    private final Element[][] paths = new Element[MAX_DEPTH][64];
    private int depth;
    private int fired;

    public int lastFired() {
        return fired;
    }

    public boolean dispatch(Event e, boolean input) {
        if (depth >= MAX_DEPTH) return false;
        Element[] path = paths[depth];
        int n = 0;
        for (Element el = e.target(); el != null; el = el.parent()) {
            if (n == path.length) {
                path = java.util.Arrays.copyOf(path, n * 2);
                paths[depth] = path;
            }
            path[n++] = el;
        }
        int skip = input ? disabledPrefix(path, n) : 0;
        depth++;
        int count = 0;
        try {
            for (int i = n - 1; i >= Math.max(1, skip); i--) {
                count += fire(path[i], e, Event.CAPTURE, true);
                if (e.stopped()) return finish(count, e);
            }
            if (skip == 0) {
                count += fire(path[0], e, Event.TARGET, false);
                e.enter(path[0], Event.TARGET);
                if (!e.prevented()) path[0].behavior().handle(path[0], e);
                if (e.stopped() || !e.bubbles()) return finish(count, e);
            }
            for (int i = Math.max(1, skip); i < n; i++) {
                count += fire(path[i], e, Event.BUBBLE, false);
                e.enter(path[i], Event.BUBBLE);
                if (!e.prevented()) path[i].behavior().handle(path[i], e);
                if (e.stopped()) break;
            }
            return finish(count, e);
        } finally {
            depth--;
            for (int i = 0; i < n; i++) path[i] = null;
        }
    }

    private boolean finish(int count, Event e) {
        fired = count;
        return count > 0 || e.prevented();
    }

    public boolean emit(Element target, Event e) {
        int count = fire(target, e, Event.TARGET, false);
        fired = count;
        return count > 0;
    }

    private static int fire(Element el, Event e, int phase, boolean captureOnly) {
        List<Handler> handlers = el.handlers();
        int count = 0;
        for (int i = 0; i < handlers.size(); i++) {
            Handler h = handlers.get(i);
            if (!h.type().equals(e.type())) continue;
            if (captureOnly && !h.capture()) continue;
            if (phase == Event.BUBBLE && h.capture()) continue;
            if (!h.accepts(e)) continue;
            e.enter(el, phase);
            h.fire(e);
            count++;
            if (e.stopped() && phase != Event.TARGET) break;
        }
        return count;
    }

    private static int disabledPrefix(Element[] path, int n) {
        for (int i = n - 1; i >= 0; i--) if (path[i].disabled()) return i + 1;
        return 0;
    }
}

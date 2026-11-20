package dev.henny.vibeyclient.module.servers.worlds;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.servers.core.WorldSort;
import dev.henny.vui.bind.State;

import java.util.Locale;

public final class SortController {
    public final State<String> label = new State<>("");
    public final State<String> tip = new State<>("");
    public final State<Float> left = new State<>(0f);
    public final State<Float> top = new State<>(0f);
    private final Runnable onCycle;

    SortController(Runnable onCycle) {
        this.onCycle = onCycle;
    }

    public void cycle() {
        onCycle.run();
    }

    void show(WorldSort sort) {
        String l = Lang.tr("servers.worlds.sort." + sort.name().toLowerCase(Locale.ROOT));
        if (!l.equals(label.peek())) label.set(l);
        String t = Lang.tr("servers.worlds.sortTip");
        if (!t.equals(tip.peek())) tip.set(t);
    }

    void place(float x, float y) {
        if (left.peek() != x) left.set(x);
        if (top.peek() != y) top.set(y);
    }
}

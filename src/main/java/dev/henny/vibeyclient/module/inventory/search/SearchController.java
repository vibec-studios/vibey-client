package dev.henny.vibeyclient.module.inventory.search;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vui.bind.State;

public final class SearchController {
    public final State<String> query = new State<>("");
    public final State<Float> x = new State<>(0f);
    public final State<Float> y = new State<>(0f);
    public final State<String> count = new State<>("");
    public final State<Boolean> some = new State<>(false);

    public String tr(String key) {
        return Lang.tr(key);
    }

    public void clear() {
        query.set("");
    }

    void place(float px, float py) {
        if (x.peek() != px) x.set(px);
        if (y.peek() != py) y.set(py);
    }

    void show(int hits, boolean active) {
        String text = active ? Integer.toString(hits) : "";
        if (!text.equals(count.peek())) count.set(text);
        if (some.peek() != active) some.set(active);
    }
}

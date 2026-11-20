package dev.henny.vui.demo;

import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class DemoController {
    public final State<String> title = new State<>("vui demo");
    public final State<String> tab = new State<>("Controls");
    public final State<Boolean> sound = new State<>(true);
    public final State<Boolean> fancy = new State<>(false);
    public final State<String> size = new State<>("Medium");
    public final State<Float> volume = new State<>(0.6f);
    public final State<String> name = new State<>("");
    public final State<String> mode = new State<>("auto");
    public final State<Integer> zoomKey = new State<>(67);
    public final State<String> accent = new State<>("#8b6cff");
    public final State<Float> progress = new State<>(0.42f);
    public final State<Boolean> menu = new State<>(false);
    public final State<Boolean> confirm = new State<>(false);
    public final State<Integer> clicks = new State<>(0);
    public final ListState<String> players = new ListState<>(players(120));
    private Consumer<String> toastSink = text -> {
    };
    private Runnable hudEditor = () -> {
    };

    public DemoController toasts(Consumer<String> sink) {
        toastSink = sink;
        return this;
    }

    public DemoController hudEditor(Runnable open) {
        hudEditor = open;
        return this;
    }

    public void save() {
        toastSink.accept(name.peek().isEmpty() ? "Saved" : "Saved " + name.peek());
    }

    public void click() {
        clicks.update(n -> n + 1);
    }

    public void ask() {
        confirm.set(true);
    }

    public void remove() {
        confirm.set(false);
        toastSink.accept("Deleted");
    }

    public void hud() {
        hudEditor.run();
    }

    private static List<String> players(int count) {
        String[] names = {"Alex", "Steve", "Noor", "Kai", "Mira", "Jun", "Lea", "Ravi"};
        List<String> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(names[i % names.length] + " #" + (i + 1));
        return out;
    }
}

package dev.henny.vibeyclient.ui.vui;

import dev.henny.vui.bind.State;

import java.util.Objects;
import java.util.function.Consumer;

public final class WatchedState<T> extends State<T> {
    private Consumer<T> listener = v -> {
    };

    public WatchedState(T initial) {
        super(initial);
    }

    public WatchedState<T> onChange(Consumer<T> l) {
        listener = l;
        return this;
    }

    public void quiet(T next) {
        super.set(next);
    }

    @Override
    public void set(T next) {
        boolean changed = !Objects.equals(peek(), next);
        super.set(next);
        if (changed) listener.accept(next);
    }
}

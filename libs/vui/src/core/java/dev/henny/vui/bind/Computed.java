package dev.henny.vui.bind;

import java.util.function.Supplier;

public final class Computed<T> extends Observable implements ReadableValue<T> {
    private final Supplier<T> formula;
    private final Effect tracker;
    private T value;
    private boolean stale = true;

    public Computed(Supplier<T> formula) {
        this.formula = formula;
        this.tracker = new Effect(this::recompute, null).onDirty(this::markStale);
    }

    public static <T> Computed<T> of(Supplier<T> formula) {
        return new Computed<>(formula);
    }

    @Override
    public T get() {
        track();
        if (stale) tracker.run();
        return value;
    }

    public boolean isStale() {
        return stale;
    }

    private void recompute() {
        value = formula.get();
        stale = false;
    }

    private void markStale() {
        if (stale) return;
        stale = true;
        changed();
    }
}

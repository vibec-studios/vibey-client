package dev.henny.vui.bind;

import java.util.Objects;
import java.util.function.UnaryOperator;

public class State<T> extends Observable implements ReadableValue<T> {
    private T value;

    public State(T initial) {
        this.value = initial;
    }

    public static <T> State<T> of(T initial) {
        return new State<>(initial);
    }

    @Override
    public T get() {
        track();
        return value;
    }

    public T peek() {
        return value;
    }

    public void set(T next) {
        if (Objects.equals(value, next)) return;
        value = next;
        changed();
    }

    public void update(UnaryOperator<T> change) {
        set(change.apply(value));
    }

    public void notifyChanged() {
        changed();
    }

    @Override
    public String toString() {
        return "State(" + value + ")";
    }
}

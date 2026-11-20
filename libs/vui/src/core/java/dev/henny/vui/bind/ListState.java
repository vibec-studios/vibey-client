package dev.henny.vui.bind;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public final class ListState<T> extends Observable implements ReadableValue<List<T>> {
    private final ArrayList<T> items;
    private final List<T> view = new TrackedView();
    private int version;

    public ListState() {
        this.items = new ArrayList<>();
    }

    public ListState(Collection<? extends T> initial) {
        this.items = new ArrayList<>(initial);
    }

    @SafeVarargs
    public static <T> ListState<T> of(T... initial) {
        return new ListState<>(List.of(initial));
    }

    @Override
    public List<T> get() {
        track();
        return view;
    }

    public int size() {
        track();
        return items.size();
    }

    public T get(int index) {
        track();
        return items.get(index);
    }

    public int version() {
        return version;
    }

    public void add(T item) {
        items.add(item);
        bump();
    }

    public void add(int index, T item) {
        items.add(index, item);
        bump();
    }

    public void set(int index, T item) {
        items.set(index, item);
        bump();
    }

    public T remove(int index) {
        T removed = items.remove(index);
        bump();
        return removed;
    }

    public boolean remove(T item) {
        boolean removed = items.remove(item);
        if (removed) bump();
        return removed;
    }

    public boolean removeIf(Predicate<? super T> filter) {
        boolean removed = items.removeIf(filter);
        if (removed) bump();
        return removed;
    }

    public void move(int from, int to) {
        T item = items.remove(from);
        items.add(to, item);
        bump();
    }

    public void replaceAll(Collection<? extends T> next) {
        items.clear();
        items.addAll(next);
        bump();
    }

    public void clear() {
        if (items.isEmpty()) return;
        items.clear();
        bump();
    }

    public void notifyChanged() {
        bump();
    }

    public List<T> snapshot() {
        return new ArrayList<>(items);
    }

    private void bump() {
        version++;
        changed();
    }

    private final class TrackedView extends AbstractList<T> {
        @Override
        public T get(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }
}

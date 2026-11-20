package dev.henny.vibeyclient.module.inventory.core;

import java.util.ArrayDeque;

public final class BoundedQueue<T> {
    private final ArrayDeque<T> items = new ArrayDeque<>();
    private final int capacity;

    public BoundedQueue(int capacity) {
        this.capacity = capacity;
    }

    public boolean offer(T item) {
        if (items.size() >= capacity) return false;
        items.addLast(item);
        return true;
    }

    public T peek() {
        return items.peekFirst();
    }

    public T poll() {
        return items.pollFirst();
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }
}

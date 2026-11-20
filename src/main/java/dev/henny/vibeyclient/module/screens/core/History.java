package dev.henny.vibeyclient.module.screens.core;

import java.util.ArrayDeque;
import java.util.Deque;

public final class History<T> {
    public static final int LIMIT = 100;
    public static final long MERGE_MS = 800;

    private final Deque<T> undo = new ArrayDeque<>();
    private final Deque<T> redo = new ArrayDeque<>();
    private String lastTag;
    private long lastAt;

    public void record(T before, String tag, long nowMs) {
        boolean merge = tag != null && tag.equals(lastTag) && nowMs - lastAt < MERGE_MS && !undo.isEmpty();
        if (!merge) {
            undo.push(before);
            while (undo.size() > LIMIT) undo.removeLast();
        }
        redo.clear();
        lastTag = tag;
        lastAt = nowMs;
    }

    public T undo(T current) {
        if (undo.isEmpty()) return null;
        redo.push(current);
        lastTag = null;
        return undo.pop();
    }

    public T redo(T current) {
        if (redo.isEmpty()) return null;
        undo.push(current);
        lastTag = null;
        return redo.pop();
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public void clear() {
        undo.clear();
        redo.clear();
        lastTag = null;
    }
}

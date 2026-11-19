package dev.henny.r3.interact;

import java.util.ArrayDeque;
import java.util.Deque;

public final class UndoHistory {
    private final Deque<UndoEntry> entries = new ArrayDeque<>();
    private final int depth;
    private final float coalesceSeconds;

    public UndoHistory(int depth, float coalesceSeconds) {
        this.depth = Math.max(1, depth);
        this.coalesceSeconds = coalesceSeconds;
    }

    public void push(Interactable target, PoseSnapshot before, UndoKind kind, float now) {
        UndoEntry top = entries.peekFirst();
        if (kind == UndoKind.WHEEL_RESIZE && top != null && top.target() == target && top.kind() == kind
                && now - top.time() <= coalesceSeconds) {
            top.touch(now);
            return;
        }
        entries.addFirst(new UndoEntry(target, before, kind, now));
        while (entries.size() > depth) entries.removeLast();
    }

    public UndoEntry pop() {
        return entries.pollFirst();
    }

    public UndoEntry peek() {
        return entries.peekFirst();
    }

    public int size() {
        return entries.size();
    }

    public void forget(Interactable target) {
        entries.removeIf(e -> e.target() == target);
    }

    public void clear() {
        entries.clear();
    }
}

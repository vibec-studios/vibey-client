package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.ui.hud.HudLayout;

import java.util.ArrayDeque;

public final class History {
    static final int MAX = 100;
    static final long MERGE_MS = 800;

    private final ArrayDeque<HudLayout.State> undo = new ArrayDeque<>();
    private final ArrayDeque<HudLayout.State> redo = new ArrayDeque<>();
    private Object lastKey;
    private long lastAt;

    public void checkpoint(Object key) {
        long now = System.currentTimeMillis();
        if (key != null && key.equals(lastKey) && now - lastAt < MERGE_MS) {
            lastAt = now;
            return;
        }
        lastKey = key;
        lastAt = now;
        push(HudLayout.capture());
    }

    public void push(HudLayout.State s) {
        undo.push(s);
        while (undo.size() > MAX) undo.removeLast();
        redo.clear();
    }

    public void pushIfChanged(HudLayout.State before) {
        if (before == null || before.sameAs(HudLayout.capture())) return;
        push(before);
        lastKey = null;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public boolean undo() {
        if (undo.isEmpty()) return false;
        redo.push(HudLayout.capture());
        HudLayout.restore(undo.pop());
        lastKey = null;
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) return false;
        undo.push(HudLayout.capture());
        HudLayout.restore(redo.pop());
        lastKey = null;
        return true;
    }

    public void breakMerge() {
        lastKey = null;
    }
}

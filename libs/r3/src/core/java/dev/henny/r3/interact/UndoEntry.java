package dev.henny.r3.interact;

public final class UndoEntry {
    private final Interactable target;
    private final PoseSnapshot before;
    private final UndoKind kind;
    private float time;

    public UndoEntry(Interactable target, PoseSnapshot before, UndoKind kind, float time) {
        this.target = target;
        this.before = before;
        this.kind = kind;
        this.time = time;
    }

    public Interactable target() {
        return target;
    }

    public PoseSnapshot before() {
        return before;
    }

    public UndoKind kind() {
        return kind;
    }

    public float time() {
        return time;
    }

    void touch(float time) {
        this.time = time;
    }
}

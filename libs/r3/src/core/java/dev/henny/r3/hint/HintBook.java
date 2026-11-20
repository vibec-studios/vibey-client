package dev.henny.r3.hint;

public final class HintBook {
    private final HintStore store;
    private final float maxSeconds;
    private final float[] shownFor = new float[Hint.values().length];
    private Hint current;

    public HintBook(HintStore store, float maxSeconds) {
        this.store = store;
        this.maxSeconds = maxSeconds;
    }

    public void update(Hint situation, float dt) {
        current = situation != null && !store.seen(situation.id()) ? situation : null;
        if (current == null) return;
        shownFor[current.ordinal()] += dt;
        if (shownFor[current.ordinal()] >= maxSeconds) store.markSeen(current.id());
    }

    public void performed(Hint hint) {
        store.markSeen(hint.id());
        if (current == hint) current = null;
    }

    public Hint current() {
        return current;
    }

    public HintStore store() {
        return store;
    }
}

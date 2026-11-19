package dev.henny.vui.bind;

public class Effect implements Observer {
    private static final Observable[] NONE = new Observable[0];

    private final Runnable body;
    private Scheduler scheduler;
    private Runnable onDirty;
    private Observable[] dependencies = NONE;
    private int dependencyCount;
    private boolean dirty;
    private boolean untracked;
    private boolean disposed;

    public Effect(Runnable body, Scheduler scheduler) {
        this.body = body;
        this.scheduler = scheduler;
    }

    public Effect onDirty(Runnable callback) {
        this.onDirty = callback;
        return this;
    }

    public void run() {
        if (disposed) return;
        clearDependencies();
        untracked = false;
        dirty = false;
        Effect previous = Tracker.swap(this);
        try {
            body.run();
        } finally {
            Tracker.swap(previous);
        }
    }

    @Override
    public void dependencyChanged() {
        if (dirty || disposed) return;
        dirty = true;
        if (onDirty != null) onDirty.run();
        else if (scheduler != null) scheduler.schedule(this);
        else run();
    }

    public void invalidate() {
        dependencyChanged();
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean readsUntrackedValues() {
        return untracked;
    }

    public int dependencyCount() {
        return dependencyCount;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void dispose() {
        disposed = true;
        clearDependencies();
        if (scheduler != null) scheduler.forget(this);
    }

    void markUntracked() {
        untracked = true;
    }

    void depend(Observable observable) {
        for (int i = 0; i < dependencyCount; i++) if (dependencies[i] == observable) return;
        if (dependencyCount == dependencies.length) {
            Observable[] grown = new Observable[Math.max(4, dependencyCount * 2)];
            System.arraycopy(dependencies, 0, grown, 0, dependencyCount);
            dependencies = grown;
        }
        dependencies[dependencyCount++] = observable;
        observable.subscribe(this);
    }

    private void clearDependencies() {
        for (int i = 0; i < dependencyCount; i++) {
            dependencies[i].unsubscribe(this);
            dependencies[i] = null;
        }
        dependencyCount = 0;
    }
}

package dev.henny.vui.bind;

public abstract class Observable {
    private static final Observer[] NONE = new Observer[0];
    private Observer[] observers = NONE;

    public final void subscribe(Observer observer) {
        Observer[] current = observers;
        for (Observer o : current) if (o == observer) return;
        Observer[] next = new Observer[current.length + 1];
        System.arraycopy(current, 0, next, 0, current.length);
        next[current.length] = observer;
        observers = next;
    }

    public final void unsubscribe(Observer observer) {
        Observer[] current = observers;
        int index = -1;
        for (int i = 0; i < current.length; i++) if (current[i] == observer) index = i;
        if (index < 0) return;
        if (current.length == 1) {
            observers = NONE;
            return;
        }
        Observer[] next = new Observer[current.length - 1];
        System.arraycopy(current, 0, next, 0, index);
        System.arraycopy(current, index + 1, next, index, current.length - index - 1);
        observers = next;
    }

    public final int observerCount() {
        return observers.length;
    }

    protected final void track() {
        Tracker.track(this);
    }

    protected final void changed() {
        Observer[] snapshot = observers;
        for (Observer o : snapshot) o.dependencyChanged();
    }
}

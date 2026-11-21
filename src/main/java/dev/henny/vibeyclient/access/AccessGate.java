package dev.henny.vibeyclient.access;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class AccessGate {
    private final AccessSource source;
    private final AccessCache cache;
    private final LongSupplier clock;
    private final List<Consumer<AccessView>> listeners = new CopyOnWriteArrayList<>();
    private AccessView view = AccessView.START;
    private int generation;

    public AccessGate(AccessSource source, AccessCache cache, LongSupplier clock) {
        this.source = source;
        this.cache = cache;
        this.clock = clock;
    }

    public synchronized AccessView view() {
        return view;
    }

    public boolean open() {
        return view().open();
    }

    public AutoCloseable onChange(Consumer<AccessView> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void check(boolean userAction) {
        run(userAction, true);
    }

    public void denied(String code) {
        String key = source.accountKey();
        boolean wasOpen;
        synchronized (this) {
            wasOpen = view.open();
            generation++;
        }
        cache.forget(key);
        publish(new AccessView(AccessPhase.DENIED, AccessInfo.denied(code), wasOpen));
        if (wasOpen) run(false, false);
    }

    private void run(boolean userAction, boolean mayOpen) {
        String key = source.accountKey();
        boolean fresh = mayOpen && cache.fresh(key, clock.getAsLong());
        int gen;
        AccessView now;
        synchronized (this) {
            gen = ++generation;
            now = view;
        }
        if (!source.online()) {
            finish(gen, fresh ? AccessPhase.OPEN : AccessPhase.ONLINE_OFF, now.info());
            return;
        }
        AccessPhase shown = fresh || mayOpen && now.open() ? AccessPhase.OPEN : now.phase();
        publishIf(gen, new AccessView(shown, now.info(), true));
        source.fetch(userAction).whenComplete((info, error) -> {
            if (error == null && info != null) answered(gen, key, info, mayOpen);
            else failed(gen, key, AccessFailure.of(error), mayOpen);
        });
    }

    private void answered(int gen, String key, AccessInfo info, boolean mayOpen) {
        if (!current(gen)) return;
        if (info.approved() && mayOpen) {
            cache.remember(key, clock.getAsLong());
            finish(gen, AccessPhase.OPEN, info);
        } else if (info.approved()) {
            finish(gen, AccessPhase.DENIED, view().info());
        } else {
            cache.forget(key);
            finish(gen, AccessPhase.DENIED, info);
        }
    }

    private void failed(int gen, String key, AccessFailure f, boolean mayOpen) {
        if (!current(gen)) return;
        if (f.kind == AccessFailure.Kind.DENIED) {
            cache.forget(key);
            finish(gen, AccessPhase.DENIED, AccessInfo.denied(f.code));
            return;
        }
        boolean fresh = mayOpen && cache.fresh(key, clock.getAsLong());
        AccessPhase p = fresh ? AccessPhase.OPEN : !mayOpen ? AccessPhase.DENIED
                : f.kind == AccessFailure.Kind.NO_LOGIN ? AccessPhase.NO_LOGIN : AccessPhase.UNREACHABLE;
        finish(gen, p, view().info());
    }

    private synchronized boolean current(int gen) {
        return gen == generation;
    }

    private void finish(int gen, AccessPhase phase, AccessInfo info) {
        publishIf(gen, new AccessView(phase, info, false));
    }

    private void publishIf(int gen, AccessView next) {
        synchronized (this) {
            if (gen != generation || next.equals(view)) return;
            view = next;
        }
        notifyListeners(next);
    }

    private void publish(AccessView next) {
        synchronized (this) {
            if (next.equals(view)) return;
            view = next;
        }
        notifyListeners(next);
    }

    private void notifyListeners(AccessView v) {
        for (Consumer<AccessView> l : listeners) l.accept(v);
    }
}

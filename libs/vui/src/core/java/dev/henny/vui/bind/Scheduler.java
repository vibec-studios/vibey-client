package dev.henny.vui.bind;

import java.util.ArrayList;
import java.util.List;

public final class Scheduler {
    private static final int MAX_PASSES = 64;

    private final List<Effect> queue = new ArrayList<>();
    private final List<Effect> running = new ArrayList<>();
    private int lastPasses;

    public void schedule(Effect effect) {
        queue.add(effect);
    }

    public void forget(Effect effect) {
        queue.remove(effect);
    }

    public boolean hasPending() {
        return !queue.isEmpty();
    }

    public int lastPasses() {
        return lastPasses;
    }

    public boolean flush() {
        int passes = 0;
        while (!queue.isEmpty()) {
            if (++passes > MAX_PASSES) {
                queue.clear();
                lastPasses = passes;
                return false;
            }
            running.addAll(queue);
            queue.clear();
            for (int i = 0; i < running.size(); i++) {
                Effect e = running.get(i);
                if (e.isDirty() && !e.isDisposed()) e.run();
            }
            running.clear();
        }
        lastPasses = passes;
        return true;
    }
}

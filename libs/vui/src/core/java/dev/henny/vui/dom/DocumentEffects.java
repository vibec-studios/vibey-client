package dev.henny.vui.dom;

import dev.henny.vui.bind.Effect;
import dev.henny.vui.bind.Scheduler;

import java.util.ArrayList;
import java.util.List;

public final class DocumentEffects {
    private final Scheduler scheduler;
    private final List<Effect> all = new ArrayList<>();
    private int sinceCompact;

    DocumentEffects(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Effect create(Runnable body) {
        Effect e = new Effect(body, scheduler);
        all.add(e);
        if (++sinceCompact > 512) compact();
        return e;
    }

    public void refresh() {
        for (int i = 0; i < all.size(); i++) {
            Effect e = all.get(i);
            if (!e.isDisposed() && e.readsUntrackedValues()) e.invalidate();
        }
    }

    public int count() {
        compact();
        return all.size();
    }

    private void compact() {
        sinceCompact = 0;
        all.removeIf(Effect::isDisposed);
    }
}

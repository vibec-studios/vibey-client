package dev.henny.vibeyclient.module.hud.timer;

import java.util.ArrayList;
import java.util.List;

final class TimerClock {
    static final int MAX_SPLITS = 9;

    private boolean running;
    private long startedAt;
    private long banked;
    private boolean rang;
    private final List<Long> splits = new ArrayList<>();

    boolean running() {
        return running;
    }

    long elapsed(long now) {
        return banked + (running ? now - startedAt : 0);
    }

    void toggle(long now) {
        if (running) {
            banked += now - startedAt;
            running = false;
        } else {
            startedAt = now;
            running = true;
        }
    }

    void lapOrReset(long now) {
        if (!running) {
            reset();
            return;
        }
        splits.add(elapsed(now));
        while (splits.size() > MAX_SPLITS) splits.remove(0);
    }

    void reset() {
        running = false;
        banked = 0;
        rang = false;
        splits.clear();
    }

    List<Long> splits() {
        return splits;
    }

    long left(long now, long length) {
        return Math.max(0, length - elapsed(now));
    }

    boolean finishOnce(long now, long length) {
        if (rang || !running || left(now, length) > 0) return false;
        rang = true;
        banked = length;
        running = false;
        return true;
    }

    boolean finished(long now, long length) {
        return rang && left(now, length) == 0;
    }
}

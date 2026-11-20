package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Clearance;
import dev.henny.vibeyclient.module.cinematic.core.Ease;
import dev.henny.vibeyclient.module.cinematic.core.Pose;

public final class Session {
    public enum Phase { INTRO, RUN, FLY_BACK, FADE_BACK, DONE }

    public record Frame(Pose pose, double fade, double bars, Phase phase) {
        public boolean done() {
            return phase == Phase.DONE;
        }
    }

    static final double INTRO = 0.6, FLY = 1.1, HALF = 0.35;

    private final Director director;
    private Phase phase = Phase.INTRO;
    private double t;
    private Pose from;
    private double fadeFrom;
    private double barsFrom = 1;
    private Frame last;

    public Session(Director director) {
        this.director = director;
    }

    public Frame update(Scene s, Pose player, double dt) {
        t += Math.max(0, dt);
        last = switch (phase) {
            case INTRO -> intro(s, player, dt);
            case RUN -> new Frame(director.update(s, dt), director.fade(), 1, phase);
            case FLY_BACK -> flyBack(player);
            case FADE_BACK -> fadeBack(player);
            case DONE -> new Frame(player, 0, 0, phase);
        };
        return last;
    }

    private Frame intro(Scene s, Pose player, double dt) {
        double u = Ease.smooth(t / INTRO);
        if (t < INTRO) return new Frame(player, u, u, phase);
        phase = Phase.RUN;
        t = 0;
        return new Frame(director.update(s, dt), director.fade(), 1, phase);
    }

    private Frame flyBack(Pose player) {
        double u = Ease.smoother(t / FLY);
        if (t >= FLY) return finish(player);
        return new Frame(from.lerp(player, u), fadeFrom * (1 - Ease.clamp01(t / 0.3)), barsFrom * (1 - u), phase);
    }

    private Frame fadeBack(Pose player) {
        if (t >= 2 * HALF) return finish(player);
        double bars = barsFrom * (1 - Ease.smooth(t / (2 * HALF)));
        if (t < HALF) return new Frame(from, Math.max(fadeFrom, Ease.smooth(t / HALF)), bars, phase);
        return new Frame(player, 1 - Ease.smooth((t - HALF) / HALF), bars, phase);
    }

    private Frame finish(Pose player) {
        phase = Phase.DONE;
        return new Frame(player, 0, 0, phase);
    }

    public void stop(Scene s) {
        if (phase == Phase.DONE || phase == Phase.FLY_BACK || phase == Phase.FADE_BACK) return;
        from = last == null ? null : last.pose();
        fadeFrom = last == null ? 0 : last.fade();
        barsFrom = last == null ? 0 : last.bars();
        t = 0;
        if (from == null) {
            phase = Phase.DONE;
            return;
        }
        if (phase == Phase.INTRO) {
            phase = Phase.FADE_BACK;
            t = HALF + (1 - fadeFrom) * HALF;
            return;
        }
        boolean fly = fadeFrom < 0.5 && fadeFrom < 0.5 && from.pos().dist(s.eye()) < 40
                && Clearance.sight(s.world(), s.eye(), from.pos());
        phase = fly ? Phase.FLY_BACK : Phase.FADE_BACK;
    }

    public void abort() {
        phase = Phase.DONE;
    }

    public Phase phase() {
        return phase;
    }

    public boolean running() {
        return phase == Phase.INTRO || phase == Phase.RUN;
    }

    public Director director() {
        return director;
    }
}

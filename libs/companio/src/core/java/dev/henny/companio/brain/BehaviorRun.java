package dev.henny.companio.brain;

import dev.henny.companio.def.BehaviorDef;
import dev.henny.companio.def.LookMode;
import dev.henny.companio.def.MoveMode;
import dev.henny.companio.def.Step;

import java.util.ArrayList;
import java.util.List;

public final class BehaviorRun {
    public final BehaviorDef def;
    public final double started;
    final double endsAt;
    final double[] nextRepeat;
    final List<Pending> pending = new ArrayList<>();
    final List<String> layers = new ArrayList<>();
    MoveMode move;
    LookMode look;
    boolean stop;
    boolean forced;

    record Pending(double time, Step step) {}

    BehaviorRun(BehaviorDef def, double now, double endsAt) {
        this.def = def;
        this.started = now;
        this.endsAt = endsAt;
        this.nextRepeat = new double[def.repeat().size()];
        this.move = def.move();
        this.look = def.look();
    }

    public double time(double now) {
        return now - started;
    }

    public MoveMode move() {
        return move;
    }

    public LookMode look() {
        return look;
    }

    boolean ended(BrainScope scope, double now) {
        if (stop) return true;
        if (!Double.isNaN(endsAt)) return now >= endsAt;
        if (time(now) < def.minDuration()) return false;
        try {
            return !def.whileExpr().test(scope);
        } catch (RuntimeException e) {
            return true;
        }
    }
}

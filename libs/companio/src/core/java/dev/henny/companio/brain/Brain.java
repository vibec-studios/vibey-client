package dev.henny.companio.brain;

import dev.henny.companio.def.BehaviorDef;
import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.def.Step;
import dev.henny.companio.def.action.StopAnimAction;
import dev.henny.companio.expr.Expr;
import dev.henny.companio.sense.Senses;
import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bundle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Brain {
    private static final float TRIGGERED_DEFAULT = 3f;
    private static final int MAX_SWITCHES_PER_TICK = 4;

    private final CompanionDef def;
    private final BrainScope scope;
    private final Memory memory;
    private final Needs needs;
    private final ActionRunner actions;
    private final Set<String> fired = new HashSet<>();
    private final Deque<String> recent = new ArrayDeque<>();
    private final List<BehaviorDef> ties = new ArrayList<>();
    private Consumer<String> log = s -> {};
    private BehaviorRun run;
    private int switches;

    public Brain(CompanionDef def, Senses senses, Self self, Random random, Supplier<Effects> fx, Supplier<String> language) {
        this.def = def;
        this.needs = new Needs(def.needs());
        this.memory = new Memory(def.vars());
        this.scope = new BrainScope(senses, self, needs, memory, random);
        this.actions = new ActionRunner(def, scope, fx, language, this);
    }

    public void props(Predicate<String> shown) {
        scope.props = shown;
    }

    public void logTo(Consumer<String> log) {
        this.log = log;
    }

    void log(String s) {
        log.accept(s);
    }

    public void fire(String event) {
        fired.add(event);
    }

    public void tick(double now, float dt) {
        scope.now = now;
        scope.dt = dt;
        switches = 0;
        for (String e : fired) {
            memory.stamp(e, now);
            remember(e);
        }
        if (run != null && run.ended(scopeFor(run), now)) end();
        choose();
        if (run != null) runTimers(now);
        needs.tick(dt, run == null ? null : run.def.needs());
        fired.clear();
        scopeFor(run);
    }

    private BrainScope scopeFor(BehaviorRun r) {
        scope.behavior = r == null ? "" : r.def.id();
        scope.behaviorTime = r == null ? 0 : r.time(scope.now);
        return scope;
    }

    private void choose() {
        BehaviorDef best = null;
        ties.clear();
        for (BehaviorDef b : def.behaviors()) {
            if (!eligible(b)) continue;
            if (best == null || b.priority() > best.priority()) {
                best = b;
                ties.clear();
                ties.add(b);
            } else if (b.priority() == best.priority()) ties.add(b);
        }
        if (best == null) return;
        if (run != null && ties.contains(run.def)) return;
        if (run != null && (best.priority() <= run.def.priority() || !run.def.interrupt() || run.forced)) return;
        start(pick(), false);
    }

    private boolean eligible(BehaviorDef b) {
        if (def.trickBehavior(b.id())) return false;
        if (!memory.ready("behavior:" + b.id(), scope.now)) return false;
        if (b.triggered() && !fired.contains(b.on())) return false;
        try {
            return b.when().test(scopeFor(run));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private BehaviorDef pick() {
        if (ties.size() == 1) return ties.get(0);
        float total = 0;
        for (BehaviorDef b : ties) total += b.weight();
        float r = scope.random.nextFloat() * total;
        for (BehaviorDef b : ties) {
            r -= b.weight();
            if (r <= 0) return b;
        }
        return ties.get(ties.size() - 1);
    }

    void switchTo(String id, boolean forced) {
        BehaviorDef b = def.behavior(id);
        if (b != null && memory.ready("behavior:" + id, scope.now)) start(b, forced);
    }

    public boolean trick(String behaviorId) {
        BehaviorDef b = def.behavior(behaviorId);
        if (b == null || !memory.ready("behavior:" + behaviorId, scope.now)) return false;
        try {
            if (!b.when().test(scopeFor(run))) return false;
        } catch (RuntimeException e) {
            return false;
        }
        start(b, true);
        return true;
    }

    private void start(BehaviorDef b, boolean forced) {
        if (++switches > MAX_SWITCHES_PER_TICK) return;
        if (run != null) end();
        double endsAt = Double.NaN;
        if (b.duration() != null) endsAt = scope.now + b.duration().pick(scope.random);
        else if (b.triggered() || forced || def.trickBehavior(b.id())) endsAt = scope.now + triggeredLength(b);
        run = new BehaviorRun(b, scope.now, endsAt);
        run.forced = forced;
        for (int i = 0; i < run.nextRepeat.length; i++) run.nextRepeat[i] = scope.now + b.repeat().get(i).every().pick(scope.random);
        memory.stamp("behavior:" + b.id(), scope.now);
        remember("behavior:" + b.id());
        if (b.anim() != null) actions.behaviorAnim(run);
        actions.run(b.start(), run, true);
    }

    private float triggeredLength(BehaviorDef b) {
        if (b.anim() == null) return TRIGGERED_DEFAULT;
        Bundle m = def.model(null);
        Animation a = m == null ? null : m.animation(b.anim().name());
        if (a == null || a.loop || a.length <= 0) return TRIGGERED_DEFAULT;
        return Math.max(0.3f, a.length / b.anim().speed());
    }

    private void end() {
        BehaviorRun r = run;
        run = null;
        if (r.def.cooldown() > 0) memory.cooldown("behavior:" + r.def.id(), r.def.cooldown(), scope.now);
        for (String layer : r.layers) actions.execute(new Step(new StopAnimAction(layer), Expr.TRUE, 1f, 0f), null);
        actions.run(r.def.end(), null, false);
    }

    private void runTimers(double now) {
        BehaviorRun r = run;
        for (int i = 0; i < r.pending.size() && run == r; ) {
            BehaviorRun.Pending p = r.pending.get(i);
            if (p.time() <= now) {
                r.pending.remove(i);
                actions.execute(p.step(), r);
            } else i++;
        }
        for (int i = 0; i < r.nextRepeat.length && run == r; i++) {
            if (now < r.nextRepeat[i]) continue;
            r.nextRepeat[i] = now + r.def.repeat().get(i).every().pick(scope.random);
            actions.run(r.def.repeat().get(i).actions(), r, true);
        }
    }

    private void remember(String e) {
        recent.addFirst(e);
        while (recent.size() > 6) recent.removeLast();
    }

    public BehaviorRun current() {
        return run;
    }

    public Needs needs() {
        return needs;
    }

    public Memory memory() {
        return memory;
    }

    public List<String> recentEvents() {
        return List.copyOf(recent);
    }

    public BrainScope scope() {
        return scope;
    }
}

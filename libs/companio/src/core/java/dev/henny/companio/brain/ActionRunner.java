package dev.henny.companio.brain;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.def.Step;
import dev.henny.companio.def.action.*;
import dev.henny.companio.text.Sanitize;

import java.util.List;
import java.util.function.Supplier;

final class ActionRunner {
    private final CompanionDef def;
    private final BrainScope scope;
    private final Supplier<Effects> fx;
    private final Supplier<String> language;
    private final Brain brain;

    ActionRunner(CompanionDef def, BrainScope scope, Supplier<Effects> fx, Supplier<String> language, Brain brain) {
        this.def = def;
        this.scope = scope;
        this.fx = fx;
        this.language = language;
        this.brain = brain;
    }

    void run(List<Step> steps, BehaviorRun owner, boolean timed) {
        for (Step s : steps) {
            if (timed && s.at() > 0 && owner != null) owner.pending.add(new BehaviorRun.Pending(scope.now + s.at(), s));
            else execute(s, owner);
        }
    }

    void behaviorAnim(BehaviorRun owner) {
        fx.get().anim(owner.def.anim(), true);
        owner.layers.add(owner.def.anim().name());
    }

    void execute(Step s, BehaviorRun owner) {
        if (s.chance() < 1f && scope.random.nextFloat() >= s.chance()) return;
        try {
            if (!s.condition().test(scope)) return;
            act(s.action(), owner);
        } catch (RuntimeException e) {
            brain.log("action " + s.action().getClass().getSimpleName() + " failed: " + e);
        }
    }

    private void act(Action a, BehaviorRun owner) {
        Effects e = fx.get();
        switch (a) {
            case AnimAction x -> {
                e.anim(x.anim(), false);
                if (owner != null) owner.layers.add(x.anim().name());
            }
            case StopAnimAction x -> e.stopAnim(x.name());
            case PropAction x -> e.prop(x.prop(), x.show());
            case SoundAction x -> e.sound(x.sound());
            case ParticleAction x -> e.particle(x.particle());
            case SayAction x -> e.say(Sanitize.bubble(x.text().resolve(def.lang(), language.get(), scope.random)), x.seconds());
            case NeedAction x -> x.deltas().forEach(scope.needs::add);
            case VarAction x -> scope.memory.var(x.var(), x.value().number(scope));
            case CooldownAction x -> scope.memory.cooldown("named:" + x.name(), x.seconds(), scope.now);
            case BehaviorAction x -> brain.switchTo(x.behavior(), false);
            case StopAction x -> {
                if (owner != null) owner.stop = true;
            }
            case MoveAction x -> {
                if (owner != null) owner.move = x.mode();
            }
            case LookAction x -> {
                if (owner != null) owner.look = x.look();
            }
            case HopAction x -> e.hop(x.height());
            case RandomAction x -> {
                if (!x.options().isEmpty()) run(x.options().get(scope.random.nextInt(x.options().size())), owner, true);
            }
            default -> { }
        }
    }
}

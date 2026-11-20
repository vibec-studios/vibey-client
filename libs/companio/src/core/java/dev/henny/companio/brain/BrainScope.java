package dev.henny.companio.brain;

import dev.henny.companio.expr.Scope;
import dev.henny.companio.expr.Values;
import dev.henny.companio.sense.Owner;
import dev.henny.companio.sense.Senses;

import java.util.Random;
import java.util.function.Predicate;

public final class BrainScope implements Scope {
    final Senses senses;
    final Self self;
    final Needs needs;
    final Memory memory;
    final Random random;
    Predicate<String> props = p -> false;
    String behavior = "";
    double now, behaviorTime;
    float dt = 0.05f;

    public BrainScope(Senses senses, Self self, Needs needs, Memory memory, Random random) {
        this.senses = senses;
        this.self = self;
        this.needs = needs;
        this.memory = memory;
        this.random = random;
    }

    @Override
    public Object variable(String name) {
        if (name.startsWith("need.")) return (double) needs.get(name.substring(5));
        if (name.startsWith("var.")) return memory.var(name.substring(4));
        if (name.startsWith("player.")) return player(name.substring(7), senses.owner);
        if (name.startsWith("self.")) return self(name.substring(5));
        return world(name);
    }

    private Object world(String name) {
        Senses s = senses;
        return switch (name) {
            case "weather.rain" -> s.raining;
            case "weather.snow" -> s.raining && s.cold;
            case "weather.thunder" -> s.thundering;
            case "weather.clear" -> !s.raining;
            case "time.day" -> !s.night();
            case "time.night" -> s.night();
            case "time.dawn" -> s.timeOfDay() >= 22500 || s.timeOfDay() < 500;
            case "time.dusk" -> s.timeOfDay() >= 11500 && s.timeOfDay() < 13500;
            case "time.hour" -> s.hour();
            case "world.dimension" -> s.dimension;
            case "world.biome" -> s.biome;
            case "world.cold" -> s.cold;
            case "world.hot" -> s.hot;
            case "world.light" -> (double) s.light;
            case "world.sky" -> s.sky;
            case "world.music" -> s.music;
            case "near.water" -> s.nearWater;
            case "near.lava" -> s.nearLava;
            case "near.fire" -> s.nearFire;
            case "near.mob" -> s.nearMob;
            case "near.player" -> s.nearPlayer;
            case "near.companion" -> s.nearCompanion;
            default -> Boolean.FALSE;
        };
    }

    private Object player(String name, Owner o) {
        return switch (name) {
            case "sneaking" -> o.sneaking;
            case "sprinting" -> o.sprinting;
            case "swimming" -> o.swimming;
            case "elytra" -> o.elytra;
            case "flying" -> o.flying;
            case "sleeping" -> o.sleeping;
            case "eating" -> o.eating;
            case "onGround" -> o.onGround;
            case "inWater" -> o.inWater;
            case "riding" -> o.riding;
            case "hurt" -> o.hurt;
            case "health" -> (double) o.health;
            case "idle" -> o.idle;
            case "speed" -> o.speed;
            case "distance" -> distance(o);
            case "looking" -> o.looking;
            case "held" -> o.held;
            case "offhand" -> o.offhand;
            case "target" -> o.target;
            case "level" -> (double) o.level;
            default -> Boolean.FALSE;
        };
    }

    private double distance(Owner o) {
        if (!o.present) return Senses.FAR;
        double dx = o.x - self.x, dy = o.y - self.y, dz = o.z - self.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private Object self(String name) {
        Self s = self;
        return switch (name) {
            case "wet" -> s.inWater || (senses.raining && s.sky && !senses.cold);
            case "speed" -> s.speed;
            case "moving" -> s.moving;
            case "onGround" -> s.onGround;
            case "inWater" -> s.inWater;
            case "onIce" -> s.onIce;
            case "onSnow" -> s.onSnow;
            case "held" -> s.held;
            case "riding" -> s.riding;
            case "command" -> s.command;
            case "behavior" -> behavior;
            case "time" -> behaviorTime;
            case "age" -> s.age;
            case "size" -> s.size;
            case "variant" -> s.variant;
            case "climb" -> s.climb;
            default -> Boolean.FALSE;
        };
    }

    @Override
    public Object call(String fn, Object[] a) {
        return switch (fn) {
            case "chance" -> random.nextDouble() < 1 - Math.pow(1 - Math.max(0, Math.min(1, Values.number(a[0]))), dt);
            case "random" -> random.nextDouble();
            case "rand" -> Values.number(a[0]) + random.nextDouble() * (Values.number(a[1]) - Values.number(a[0]));
            case "cooldown" -> memory.ready("named:" + Values.text(a[0]), now);
            case "since" -> memory.since(Values.text(a[0]), now);
            case "holding" -> holding(a);
            case "target" -> targeting(a);
            case "biome" -> contains(senses.biome, a);
            case "prop" -> props.test(Values.text(a[0]));
            case "hour" -> hour(Values.number(a[0]), Values.number(a[1]));
            default -> Boolean.FALSE;
        };
    }

    private boolean holding(Object[] a) {
        for (Object o : a) {
            String pat = Values.text(o);
            if (Glob.matches(pat, senses.owner.held) || Glob.matches(pat, senses.owner.offhand)) return true;
        }
        return false;
    }

    private boolean targeting(Object[] a) {
        for (Object o : a) if (Glob.matches(Values.text(o), senses.owner.target)) return true;
        return false;
    }

    private static boolean contains(String value, Object[] parts) {
        for (Object o : parts) if (value.contains(Values.text(o))) return true;
        return false;
    }

    private boolean hour(double from, double to) {
        double h = senses.hour();
        return from <= to ? h >= from && h < to : h >= from || h < to;
    }
}

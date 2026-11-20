package dev.henny.companio.brain;

import java.util.HashMap;
import java.util.Map;

public final class Memory {
    public static final double NEVER = 1_000_000;

    private final Map<String, Double> events = new HashMap<>();
    private final Map<String, Double> cooldowns = new HashMap<>();
    private final Map<String, Double> vars = new HashMap<>();

    public Memory(Map<String, Float> varDefaults) {
        varDefaults.forEach((k, v) -> vars.put(k, (double) v));
    }

    public void stamp(String event, double now) {
        events.put(event, now);
    }

    public double since(String event, double now) {
        Double t = events.get(event);
        return t == null ? NEVER : Math.max(0, now - t);
    }

    public void cooldown(String name, double seconds, double now) {
        cooldowns.put(name, now + seconds);
    }

    public boolean ready(String name, double now) {
        Double t = cooldowns.get(name);
        return t == null || now >= t;
    }

    public double var(String name) {
        return vars.getOrDefault(name, 0.0);
    }

    public void var(String name, double value) {
        if (vars.containsKey(name) && Double.isFinite(value)) vars.put(name, Math.max(-1e9, Math.min(1e9, value)));
    }

    public Map<String, Double> vars() {
        return Map.copyOf(vars);
    }

    public void restoreVars(Map<String, Double> saved) {
        if (saved != null) saved.forEach(this::var);
    }
}

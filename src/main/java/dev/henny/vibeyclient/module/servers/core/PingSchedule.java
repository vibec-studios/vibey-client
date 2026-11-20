package dev.henny.vibeyclient.module.servers.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PingSchedule<K> {
    public record Candidate<K>(K key, boolean favourite, boolean settled) {}

    private final Map<K, Long> lastDone = new HashMap<>();
    private final Map<K, Long> started = new HashMap<>();
    private int maxInFlight;
    private long intervalNanos;
    private final long timeoutNanos;

    public PingSchedule(int maxInFlight, long intervalMillis, long timeoutMillis) {
        configure(maxInFlight, intervalMillis);
        this.timeoutNanos = timeoutMillis * 1_000_000L;
    }

    public void configure(int maxInFlight, long intervalMillis) {
        this.maxInFlight = Math.max(1, maxInFlight);
        this.intervalNanos = Math.max(1, intervalMillis) * 1_000_000L;
    }

    public int inFlight() {
        return started.size();
    }

    public List<K> due(List<Candidate<K>> visible, long now) {
        List<K> out = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            for (Candidate<K> c : visible) {
                if (started.size() + out.size() >= maxInFlight) return out;
                if (c.favourite() != (pass == 0) || !c.settled() || started.containsKey(c.key())) continue;
                Long done = lastDone.get(c.key());
                if (done == null) lastDone.put(c.key(), now);
                else if (now - done >= intervalNanos) out.add(c.key());
            }
        }
        return out;
    }

    public void start(K key, long now) {
        started.put(key, now);
    }

    public void done(K key, long now) {
        started.remove(key);
        lastDone.put(key, now);
    }

    public List<K> expired(long now) {
        List<K> out = new ArrayList<>();
        for (Map.Entry<K, Long> e : started.entrySet()) if (now - e.getValue() >= timeoutNanos) out.add(e.getKey());
        return out;
    }

    public void forget(K key) {
        started.remove(key);
        lastDone.remove(key);
    }

    public void clear() {
        started.clear();
        lastDone.clear();
    }
}

package dev.henny.vibeafk.game;

import dev.henny.vibeafk.AfkConfig;
import dev.henny.vibeafk.Json;

import java.util.Map;

public final class GameWatcher {
    public static final long HEALTH_EVERY_MS = 30_000;

    public interface Sink {
        void event(String type, Map<String, Object> fields);
    }

    public static final class Sample {
        public final GameBridge.ScreenKind kind;
        public final String server;
        public final Float health;
        public final Float maxHealth;
        public final Integer food;
        public final String deathMessage;
        public final String disconnectReason;
        public final int screenId;

        public Sample(GameBridge.ScreenKind kind, String server, Float health, Float maxHealth, Integer food,
                      String deathMessage, String disconnectReason, int screenId) {
            this.kind = kind;
            this.server = server;
            this.health = health;
            this.maxHealth = maxHealth;
            this.food = food;
            this.deathMessage = deathMessage;
            this.disconnectReason = disconnectReason;
            this.screenId = screenId;
        }
    }

    private final Sink sink;
    private final Backoff backoff = new Backoff();
    private boolean inWorld;
    private String server;
    private boolean dead;
    private boolean healthLow;
    private boolean foodLow;
    private long lastHealthAt;
    private String lastHealthKey;
    private int lastDisconnectScreen;
    private boolean reconnectDue;
    private Sample last;

    public GameWatcher(Sink sink) {
        this.sink = sink;
    }

    public Sample last() {
        return last;
    }

    public boolean dead() {
        return dead;
    }

    public String server() {
        return server;
    }

    public Backoff backoff() {
        return backoff;
    }

    public boolean takeReconnect() {
        boolean r = reconnectDue;
        reconnectDue = false;
        return r;
    }

    public void sample(Sample s, AfkConfig cfg, long now) {
        last = s;
        boolean world = s.kind == GameBridge.ScreenKind.WORLD || s.kind == GameBridge.ScreenKind.DEATH;
        if (world) {
            if (!inWorld) {
                inWorld = true;
                server = s.server;
                dead = false;
                healthLow = false;
                foodLow = false;
                lastHealthKey = null;
                backoff.reset();
                sink.event("connect", Json.obj("server", server));
            }
            if (s.kind == GameBridge.ScreenKind.DEATH && !dead) {
                dead = true;
                sink.event("death", Json.obj("message", s.deathMessage));
            }
            health(s, cfg, now);
            return;
        }
        if (inWorld) {
            inWorld = false;
            if (s.kind == GameBridge.ScreenKind.DISCONNECTED) {
                lastDisconnectScreen = s.screenId;
                sink.event("disconnect", Json.obj("server", server, "reason", s.disconnectReason));
                if (server != null && cfg.autoReconnect) schedule(now, server, cfg);
            } else {
                sink.event("disconnect", Json.obj("server", server, "reason", null, "left", Boolean.TRUE));
                backoff.reset();
            }
            dead = false;
            return;
        }
        if (s.kind == GameBridge.ScreenKind.DISCONNECTED) {
            if (s.screenId != lastDisconnectScreen) {
                lastDisconnectScreen = s.screenId;
                String target = backoff.active() ? backoff.server() : server;
                sink.event("disconnect", Json.obj("server", target, "reason", s.disconnectReason, "failed", Boolean.TRUE));
                if (backoff.active() && cfg.autoReconnect) schedule(now, backoff.server(), cfg);
            }
            if (!cfg.autoReconnect && backoff.active()) backoff.reset();
            if (backoff.due(now)) {
                reconnectDue = true;
                sink.event("reconnect_attempt", Json.obj("attempt", backoff.attempt(), "delay_s", backoff.delayS(),
                        "server", backoff.server()));
            }
            return;
        }
        if (s.kind == GameBridge.ScreenKind.MENU) backoff.reset();
    }

    private void schedule(long now, String target, AfkConfig cfg) {
        if (!backoff.schedule(now, target, cfg.maxAttempts, cfg.baseDelayS, cfg.maxDelayS)) {
            sink.event("reconnect_gave_up", Json.obj("attempts", backoff.attempt(), "server", target));
            backoff.reset();
        }
    }

    private void health(Sample s, AfkConfig cfg, long now) {
        if (s.health == null) return;
        float h = s.health;
        if (h <= 0 && !dead) {
            dead = true;
            sink.event("death", Json.obj("message", s.deathMessage));
        } else if (h > 0 && dead && s.kind == GameBridge.ScreenKind.WORLD) {
            dead = false;
            healthLow = false;
            sink.event("respawn", null);
        }
        if (dead) return;
        if (cfg.lowHealth > 0) {
            if (!healthLow && h <= cfg.lowHealth) {
                healthLow = true;
                sink.event("health_low", Json.obj("health", (double) h, "max_health", s.maxHealth == null ? null : (double) s.maxHealth));
            } else if (healthLow && h > cfg.lowHealth + 2) {
                healthLow = false;
            }
        }
        if (cfg.lowFood > 0 && s.food != null) {
            int f = s.food;
            if (!foodLow && f <= cfg.lowFood) {
                foodLow = true;
                sink.event("food_low", Json.obj("food", f));
            } else if (foodLow && f > cfg.lowFood + 2) {
                foodLow = false;
            }
        }
        String key = h + "/" + s.maxHealth + "/" + s.food;
        if (!key.equals(lastHealthKey) && (lastHealthKey == null || now - lastHealthAt >= HEALTH_EVERY_MS)) {
            lastHealthKey = key;
            lastHealthAt = now;
            sink.event("health", Json.obj("health", (double) h, "max_health", s.maxHealth == null ? null : (double) s.maxHealth,
                    "food", s.food));
        }
    }
}

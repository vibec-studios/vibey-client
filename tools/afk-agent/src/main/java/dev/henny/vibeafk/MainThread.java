package dev.henny.vibeafk;

import dev.henny.vibeafk.game.GameBridge;
import dev.henny.vibeafk.game.GameWatcher;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

final class MainThread {
    static final long OUTBOX_MAX_AGE_MS = 60_000;

    private final Session s;
    private Core core;
    private AfkConfig applied;
    private String lastModeKey;
    private long lastSlow;
    private boolean busy;
    private Object lastServerData;
    private GameBridge.ScreenKind kind;
    private final RateLimiter limiter = new RateLimiter(3, 1500, System.currentTimeMillis());
    private final GameWatcher watcher;
    private String lastHookError;

    MainThread(final Session s) {
        this.s = s;
        this.watcher = new GameWatcher(new GameWatcher.Sink() {
            @Override
            public void event(String type, Map<String, Object> fields) {
                s.event(type, fields);
                s.markDirty();
            }
        });
    }

    void attach(Core core) {
        this.core = core;
    }

    boolean startHidden() {
        AfkConfig d = s.desired;
        return d.background && d.hideWindow;
    }

    void soundReady() {
        AfkConfig a = applied;
        if (a != null && a.background && a.mute && core.sound.muted()) core.sound.tick(s.lwjglLoader, Long.MAX_VALUE);
    }

    void hookError(String event, Throwable t) {
        String msg = event + ": " + t;
        if (msg.equals(lastHookError)) return;
        lastHookError = msg;
        s.error("hook_failed", msg);
    }

    void tick() {
        if (busy || core == null) return;
        busy = true;
        try {
            AfkConfig d = s.desired;
            if (d != applied) apply(d);
            long now = System.currentTimeMillis();
            if (now - lastSlow < 1000) return;
            lastSlow = now;
            reconcileWindow();
            core.sound.tick(s.lwjglLoader, now);
            game(now);
            drain(now);
        } catch (Throwable t) {
            hookError("tick", t);
        } finally {
            busy = false;
        }
    }

    private void apply(AfkConfig d) {
        applied = d;
        boolean bg = d.background;
        boolean wantHidden = bg && d.hideWindow;
        if (wantHidden && !core.window.hidden()) core.window.hide();
        if (!wantHidden && core.window.hidden()) core.window.show();
        core.throttle.set(bg, d.backgroundFps);
        core.sound.setMuted(bg && d.mute, s.lwjglLoader);
        modeEvent();
    }

    private void reconcileWindow() {
        if (applied == null || !core.window.available()) return;
        boolean wantHidden = applied.background && applied.hideWindow;
        boolean changed = false;
        if (wantHidden && !core.window.hidden()) changed = core.window.hide();
        else if (!wantHidden && core.window.hidden()) changed = core.window.show();
        if (changed) modeEvent();
    }

    private void modeEvent() {
        boolean bg = applied.background;
        boolean hidden = core.window.hidden() && core.window.available();
        Integer fps = bg ? Integer.valueOf(core.throttle.fps()) : null;
        boolean muted = core.sound.muted();
        String key = bg + "/" + hidden + "/" + fps + "/" + muted;
        s.appliedMode = applied.mode();
        s.markDirty();
        if (key.equals(lastModeKey)) return;
        lastModeKey = key;
        s.event("mode", Json.obj("mode", applied.mode(), "window", hidden, "fps", fps, "muted", muted));
    }

    private void game(long now) {
        GameBridge b = s.bridge;
        if (b == null) return;
        Object mc = b.minecraft();
        if (mc == null) return;
        GameBridge.ScreenKind k = b.kind(mc);
        kind = k;
        Object screen = b.rawScreen(mc);
        Object player = b.player(mc);
        boolean world = k == GameBridge.ScreenKind.WORLD || k == GameBridge.ScreenKind.DEATH;
        String server = null;
        if (world) {
            Object sd = b.serverData(mc);
            if (sd != null) {
                lastServerData = sd;
                server = b.serverIp(sd);
            }
        }
        Float h = null;
        Float mh = null;
        Integer food = null;
        if (player != null) {
            h = b.health(player);
            mh = b.maxHealth(player);
            food = b.food(player);
        }
        String death = k == GameBridge.ScreenKind.DEATH && screen != null ? b.deathMessage(screen) : null;
        String reason = k == GameBridge.ScreenKind.DISCONNECTED && screen != null ? b.disconnectReason(screen) : null;
        if (s.playerName == null) s.playerName = b.userName(mc);
        AfkConfig cfg = applied != null ? applied : s.desired;
        watcher.sample(new GameWatcher.Sample(k, server, h, mh, food, death, reason, System.identityHashCode(screen)), cfg, now);
        if (watcher.takeReconnect()) {
            try {
                Object parent = screen == null ? null : b.disconnectParent(screen);
                b.reconnect(mc, lastServerData, parent != null ? parent : screen);
            } catch (Throwable t) {
                s.error("reconnect_failed", String.valueOf(cause(t)));
                watcher.backoff().reset();
            }
        }
        s.game = Json.obj(
                "screen", k.name().toLowerCase(java.util.Locale.ROOT),
                "server", world ? server : watcher.server(),
                "player", s.playerName,
                "health", h == null ? null : Double.valueOf(Math.round(h * 10.0) / 10.0),
                "max_health", mh == null ? null : Double.valueOf(Math.round(mh * 10.0) / 10.0),
                "food", food,
                "dead", watcher.dead());
    }

    private void drain(long now) {
        OutboxItem it;
        while ((it = s.outbox.peek()) != null) {
            if (s.bridgeFailed) {
                s.outbox.poll();
                s.ack(it.id, false, "unsupported");
                continue;
            }
            boolean world = kind == GameBridge.ScreenKind.WORLD || kind == GameBridge.ScreenKind.DEATH;
            if (now - it.queuedAt > OUTBOX_MAX_AGE_MS) {
                s.outbox.poll();
                s.ack(it.id, false, s.bridge == null || !world ? "not_connected" : "rate_limited");
                continue;
            }
            GameBridge b = s.bridge;
            if (b == null || !world) return;
            if (!limiter.tryAcquire(now)) return;
            s.outbox.poll();
            try {
                Object mc = b.minecraft();
                if (mc == null || !b.send(mc, it.command, it.text)) s.ack(it.id, false, "not_connected");
                else s.ack(it.id, true, null);
            } catch (Throwable t) {
                s.ack(it.id, false, "failed");
                s.error("send_failed", String.valueOf(cause(t)));
            }
        }
    }

    private static Throwable cause(Throwable t) {
        return t instanceof InvocationTargetException && t.getCause() != null ? t.getCause() : t;
    }
}

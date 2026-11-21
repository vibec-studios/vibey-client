package dev.henny.vibeafk;

import dev.henny.vibeafk.game.GameBridge;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

final class Session {
    static final String CONFIG = "afk.json";
    static final String STATE = "afk-state.json";
    static final int QUEUE_MAX = 20;

    final File dir;
    final File gameDir;
    final Instrumentation inst;
    final long run;
    final String version;
    final String loaderKind;
    final EventLog events;
    final ConcurrentLinkedQueue<OutboxItem> outbox = new ConcurrentLinkedQueue<OutboxItem>();
    final AtomicBoolean dirty = new AtomicBoolean(true);

    volatile AfkConfig desired = AfkConfig.FOREGROUND;
    volatile GameBridge bridge;
    volatile boolean bridgeFailed;
    volatile String names = "pending";
    volatile String mcVersion;
    volatile String lwjgl;
    volatile ClassLoader lwjglLoader;
    volatile boolean lwjglLegacy;
    volatile String playerName;
    volatile Map<String, Object> game;
    volatile String appliedMode = "foreground";

    Session(File dir, Instrumentation inst, long run, String version, String loaderKind) {
        this.dir = dir;
        this.gameDir = dir.getAbsoluteFile().getParentFile();
        this.inst = inst;
        this.run = run;
        this.version = version;
        this.loaderKind = loaderKind;
        this.events = new EventLog(dir, EventLog.DEFAULT_MAX, new EventLog.Clock() {
            @Override
            public long now() {
                return System.currentTimeMillis();
            }
        });
    }

    void event(String type, Map<String, Object> fields) {
        events.emit(type, fields);
    }

    void error(String code, String message) {
        events.emit("error", Json.obj("code", code, "message", message));
    }

    void ack(String id, boolean ok, String error) {
        if (ok) events.emit("outbox_ack", Json.obj("id", id, "ok", Boolean.TRUE));
        else events.emit("outbox_ack", Json.obj("id", id, "ok", Boolean.FALSE, "error", error));
    }

    void markDirty() {
        dirty.set(true);
    }
}

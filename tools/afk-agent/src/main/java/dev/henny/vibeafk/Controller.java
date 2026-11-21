package dev.henny.vibeafk;

import dev.henny.vibeafk.game.BridgeLoader;
import dev.henny.vibeafk.game.Opener;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

final class Controller implements Runnable {
    static final long PERIOD_MS = 1000;
    static final long STATE_EVERY_MS = 5000;
    static final int CHAT_PER_TICK = 50;

    private final Session s;
    private final Core core;
    private final OutboxReader outbox;
    private final LogTail log;
    private final BridgeLoader loader;
    private final Opener opener;
    private String configStamp;
    private long lastState;
    private long lastBridgeAttempt;
    private Map<String, Object> lastCaps;
    private volatile boolean stopped;

    Controller(Session s, Core core) {
        this.s = s;
        this.core = core;
        this.outbox = new OutboxReader(s.dir);
        this.log = new LogTail(new File(new File(s.gameDir, "logs"), "latest.log"));
        this.loader = new BridgeLoader(s.inst, s.loaderKind, s.run);
        this.opener = new Opener(s.inst);
    }

    void prepare() {
        readConfig();
        outbox.poll(new OutboxReader.Sink() {
            @Override
            public void line(String line) {
                String id = null;
                try {
                    id = OutboxItem.parse(line, 0).id;
                } catch (OutboxItem.Invalid e) {
                    id = e.id;
                }
                if (id != null) s.ack(id, false, "stale");
            }

            @Override
            public void tooLarge() {
                s.error("outbox_too_large", "afk-outbox.jsonl is larger than " + OutboxReader.MAX_BYTES + " bytes");
            }
        });
        log.skipExisting();
    }

    @Override
    public void run() {
        while (!stopped) {
            try {
                once(System.currentTimeMillis());
            } catch (Throwable t) {
                s.error("agent_failed", String.valueOf(t));
            }
            Fs.sleep(PERIOD_MS);
        }
    }

    void stop() {
        stopped = true;
    }

    void once(long now) {
        readConfig();
        readOutbox(now);
        readLog();
        bridge(now);
        capabilities();
        if (s.dirty.getAndSet(false) || now - lastState >= STATE_EVERY_MS) writeState(now, true);
    }

    private void readConfig() {
        File f = new File(s.dir, Session.CONFIG);
        if (!f.isFile()) {
            if (!"missing".equals(configStamp)) {
                configStamp = "missing";
                AfkConfig prev = s.desired;
                if (prev.background) s.desired = AfkConfig.FOREGROUND;
            }
            return;
        }
        String stamp = f.lastModified() + ":" + f.length();
        if (stamp.equals(configStamp)) return;
        configStamp = stamp;
        String text = Fs.read(f, 64 * 1024);
        if (text == null) {
            configStamp = null;
            return;
        }
        try {
            AfkConfig c = AfkConfig.parse(text);
            if (c.mappings != null) loader.setMappings(c.mappings);
            s.desired = c;
            s.markDirty();
        } catch (RuntimeException e) {
            s.error("afk_json_invalid", String.valueOf(e.getMessage()));
        }
    }

    private void readOutbox(final long now) {
        outbox.poll(new OutboxReader.Sink() {
            @Override
            public void line(String line) {
                try {
                    OutboxItem it = OutboxItem.parse(line, now);
                    if (s.outbox.size() >= Session.QUEUE_MAX) s.ack(it.id, false, "rate_limited");
                    else s.outbox.add(it);
                } catch (OutboxItem.Invalid e) {
                    if (e.id != null) s.ack(e.id, false, "invalid");
                    else s.error("outbox_invalid", e.getMessage() + ": " + clip(line, 200));
                }
            }

            @Override
            public void tooLarge() {
                s.error("outbox_too_large", "afk-outbox.jsonl is larger than " + OutboxReader.MAX_BYTES + " bytes");
            }
        });
    }

    private void readLog() {
        final int[] count = {0};
        log.poll(new LogTail.Sink() {
            @Override
            public void line(String line) {
                if (s.playerName == null) {
                    String u = ChatParser.userFromLog(line);
                    if (u != null) s.playerName = u;
                }
                ChatParser.Line l = ChatParser.parseLogLine(line);
                if (l == null) return;
                if (++count[0] > CHAT_PER_TICK) return;
                s.event("chat", Json.obj("text", l.text, "sender", l.sender, "source", "log"));
                if (l.whisper) s.event("whisper", Json.obj("text", l.text, "sender", l.sender));
                else if (l.mentions(s.playerName)) s.event("mention", Json.obj("text", l.text, "sender", l.sender));
            }
        });
        if (count[0] > CHAT_PER_TICK) s.event("chat_dropped", Json.obj("count", count[0] - CHAT_PER_TICK));
    }

    private void bridge(long now) {
        if (s.bridge != null || s.bridgeFailed || now - lastBridgeAttempt < 2000) return;
        lastBridgeAttempt = now;
        BridgeLoader.Result r = loader.attempt(now, opener);
        String err = loader.takeError();
        if (err != null) {
            int colon = err.indexOf(':');
            s.error(colon > 0 ? err.substring(0, colon) : "bridge_failed", colon > 0 ? err.substring(colon + 1).trim() : err);
        }
        if (r != null) {
            s.names = r.names;
            if (s.mcVersion == null) s.mcVersion = Env.mcVersion(r.loader);
            s.bridge = r.bridge;
            s.markDirty();
        } else if (loader.failed()) {
            s.names = loader.status();
            s.bridgeFailed = true;
            s.markDirty();
        }
    }

    Map<String, Object> caps() {
        boolean bridge = s.bridge != null;
        Map<String, Object> c = new LinkedHashMap<String, Object>();
        c.put("window", core.window.available());
        c.put("throttle", core.throttle.frames() > 0);
        c.put("mute", core.sound.available());
        c.put("chat_events", log.exists());
        c.put("game_state", bridge);
        c.put("send", bridge && s.bridge.canSend());
        c.put("reconnect", bridge && s.bridge.canReconnect());
        return c;
    }

    private void capabilities() {
        if (s.lwjgl == null && s.lwjglLoader != null) s.lwjgl = Env.lwjglVersion(s.lwjglLoader, s.lwjglLegacy);
        Map<String, Object> c = caps();
        if (c.equals(lastCaps)) return;
        lastCaps = c;
        s.event("capabilities", c);
        s.markDirty();
    }

    void writeState(long now, boolean running) {
        lastState = now;
        AfkConfig d = s.desired;
        Map<String, Object> env = Json.obj(
                "lwjgl", s.lwjgl,
                "loader", s.loaderKind,
                "mc", s.mcVersion,
                "names", s.names,
                "java", System.getProperty("java.version"));
        Map<String, Object> o = Json.obj(
                "schema", 1,
                "agent_version", s.version,
                "run", s.run,
                "pid", Pid.current(),
                "session", d.session,
                "mode", s.appliedMode,
                "running", running,
                "env", env,
                "capabilities", lastCaps != null ? lastCaps : caps());
        if (s.game != null) o.put("game", s.game);
        o.put("updated_at", now);
        try {
            Fs.writeAtomic(new File(s.dir, Session.STATE), Json.write(o) + "\n");
        } catch (IOException e) {
            System.err.println("[vibe-afk] cannot write state: " + e);
        }
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}

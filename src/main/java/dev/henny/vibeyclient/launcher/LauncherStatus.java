package dev.henny.vibeyclient.launcher;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.api.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Keeps {@code <instance>/.vibe/session.json} up to date for the Vibe Launcher's Discord status (PLAY-LINKS.md §2).
 * The state is sampled on the client thread once a second (cheap reads only); the file is written on its own
 * single writer thread when something changed and every {@value #HEARTBEAT_MS} ms (so {@code updated_at} never gets
 * older than the launcher's 60 s limit). Deleted on exit (shutdown hook) and while the player switched it off.
 *
 * <p>Only written when {@code <instance>/.vibe/} exists, i.e. when the Vibe Launcher manages the instance: nobody
 * else reads the file. The instance folder is the game directory.
 */
public final class LauncherStatus {
    private LauncherStatus() {}

    static final long HEARTBEAT_MS = 15_000;

    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VibeyClient-SessionFile");
        t.setDaemon(true);
        return t;
    });
    private static final Object LOCK = new Object();

    private static Path instanceDir;
    private static int ticks;
    private static SessionFile.Snapshot written;
    private static long writtenAt;
    private static boolean fileExists;
    /** Set once by the shutdown hook: no more writes after that. Guarded by LOCK. */
    private static boolean closed;

    /** Once at startup (client thread). */
    public static void init(Path gameDir) {
        instanceDir = gameDir.toAbsolutePath().normalize();
        ServerRules.supports("discord.server");
        Runtime.getRuntime().addShutdownHook(new Thread(LauncherStatus::shutdown, "VibeyClient-SessionFile-Exit"));
        // A file left over from a crash is stale anyway; the first tick writes a fresh one.
        WRITER.execute(() -> SessionFile.delete(vibeDir()));
    }

    /** The instance folder (game directory), or null before {@link #init}. */
    public static Path instanceDir() {
        return instanceDir;
    }

    public static Path vibeDir() {
        return instanceDir.resolve(InstanceInfo.DIR);
    }

    /** Reads {@code instance.json} (blocking I/O: call it on a worker thread). */
    public static InstanceInfo instance() {
        return instanceDir == null ? InstanceInfo.absent() : InstanceInfo.read(instanceDir);
    }

    /** Every client tick. */
    public static void tick() {
        if (instanceDir == null || ++ticks % 20 != 0) return;
        update(false);
    }

    /** Samples the state now and writes it if it changed (client thread), e.g. right after a play link was made. */
    public static void changed() {
        Minecraft.getInstance().execute(() -> update(true));
    }

    private static void update(boolean force) {
        if (instanceDir == null) return;
        long now = System.currentTimeMillis();
        if (!GlobalSettings.launcherStatus.on()) {
            if (fileExists || written != null) {
                written = null;
                fileExists = false;
                submit(null);
            }
            return;
        }
        SessionFile.Snapshot s;
        try {
            s = sample();
        } catch (RuntimeException e) {
            return; // mid world change; next second
        }
        if (!force && s.equals(written) && now - writtenAt < HEARTBEAT_MS) return;
        if (force && s.equals(written) && now - writtenAt < 1000) return;
        written = s;
        writtenAt = now;
        fileExists = true;
        submit(s);
    }

    private static void submit(SessionFile.Snapshot s) {
        Path dir = vibeDir();
        long at = System.currentTimeMillis() / 1000L;
        try {
            WRITER.execute(() -> {
                synchronized (LOCK) {
                    if (closed) return;
                    if (s == null) {
                        SessionFile.delete(dir);
                        return;
                    }
                    if (!Files.isDirectory(dir)) return; // not a Vibe Launcher instance
                    try {
                        SessionFile.write(dir, s, at);
                    } catch (java.io.IOException e) {
                        Vibey.LOG.debug("session.json not written: {}", e.toString());
                    }
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
        }
    }

    /** What the game does right now (client thread). */
    static SessionFile.Snapshot sample() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return SessionFile.Snapshot.menu();
        IntegratedServer sp = mc.getSingleplayerServer();
        if (sp != null) {
            String world = sp.getWorldData().getLevelName();
            HostingService hs = Services.get(HostingService.class).orElse(null);
            if (hs != null && hs.isHosting()) {
                int players = hs.players();
                int max = hs.maxPlayers();
                return new SessionFile.Snapshot(SessionFile.State.HOSTING, null, world, hs.playLink(),
                        players >= 0 ? players : null, max > 0 ? max : null);
            }
            return new SessionFile.Snapshot(SessionFile.State.SINGLEPLAYER, null, world, null, null, null);
        }
        ServerData d = mc.getCurrentServer();
        String server = d == null || hideServer() ? null : d.ip;
        ClientPacketListener conn = mc.getConnection();
        Integer players = conn == null ? null : conn.getOnlinePlayers().size();
        return new SessionFile.Snapshot(SessionFile.State.MULTIPLAYER, server, null, null, players, null);
    }

    /** The server asked us to (rule {@code discord.server}) or the player hides server addresses (social privacy). */
    static boolean hideServer() {
        if (ServerRules.isDisabled("discord.server")) return true;
        VibeyModule social = ModuleManager.get().get("social");
        if (social == null) return false;
        Setting<?, ?> hide = social.settings().get("hideServerAddress");
        return hide != null && Boolean.TRUE.equals(hide.get());
    }

    private static void shutdown() {
        WRITER.shutdown();
        try {
            WRITER.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (LOCK) {
            closed = true;
            if (instanceDir != null) SessionFile.delete(vibeDir());
        }
    }
}

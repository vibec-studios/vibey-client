package dev.henny.vibeyclient.module.skins;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.launcher.LauncherStatus;
import dev.henny.vibeyclient.module.skins.core.BridgeFiles;
import dev.henny.vibeyclient.module.skins.core.LibrarySkin;
import dev.henny.vibeyclient.module.skins.core.SkinLibrary;
import dev.henny.vibeyclient.module.skins.core.SkinRequest;
import dev.henny.vibeyclient.module.skins.core.SkinResult;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SkinSwap {
    public enum Phase { IDLE, SENT, WORKING, DONE, FAILED }

    public enum Availability { LOADING, NO_LAUNCHER, NO_LIST, WRONG_ACCOUNT, CANNOT_CHANGE, READY }

    static final long ANSWER_TIMEOUT_MS = 8_000;
    static final long WORK_TIMEOUT_MS = 45_000;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vibey skins io");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private volatile SkinLibrary library;
    private volatile boolean launcher;
    private volatile boolean scanned;
    private volatile long libraryMtime = Long.MIN_VALUE;
    private volatile Phase phase = Phase.IDLE;
    private volatile String requestId;
    private volatile String requestSkin;
    private volatile long sentAt;
    private volatile long workingAt;
    private volatile String failure;
    private volatile boolean polling;
    private int ticks;
    private volatile long lastRefresh;

    public SkinLibrary library() {
        return library;
    }

    public Phase phase() {
        return phase;
    }

    public String pendingSkin() {
        return busy() ? requestSkin : null;
    }

    public String failure() {
        return failure;
    }

    public boolean busy() {
        return phase == Phase.SENT || phase == Phase.WORKING;
    }

    public Availability availability() {
        if (!scanned) return Availability.LOADING;
        if (!launcher) return Availability.NO_LAUNCHER;
        SkinLibrary l = library;
        if (l == null) return Availability.NO_LIST;
        UUID me = localUuid();
        if (l.accountUuid() != null && me != null && !sameUuid(l.accountUuid(), me)) return Availability.WRONG_ACCOUNT;
        if (!l.canChange()) return Availability.CANNOT_CHANGE;
        return Availability.READY;
    }

    static boolean sameUuid(String a, UUID b) {
        return a.replace("-", "").equalsIgnoreCase(b.toString().replace("-", ""));
    }

    static UUID localUuid() {
        try {
            return Minecraft.getInstance().getUser().getProfileId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    static Path vibeDir() {
        return LauncherStatus.instanceDir() == null ? null : LauncherStatus.vibeDir();
    }

    void startup() {
        IO.execute(() -> {
            Path d = vibeDir();
            if (d != null && Files.isDirectory(d)) BridgeFiles.clearOwn(d);
        });
    }

    public void refresh() {
        lastRefresh = System.currentTimeMillis();
        IO.execute(this::scan);
    }

    public void refreshIfStale(long maxAgeMs) {
        if (System.currentTimeMillis() - lastRefresh > maxAgeMs) refresh();
    }

    private void scan() {
        Path d = vibeDir();
        launcher = d != null && Files.isDirectory(d);
        if (launcher) {
            long m = BridgeFiles.mtime(d.resolve(BridgeFiles.LIBRARY));
            if (m != libraryMtime) {
                libraryMtime = m;
                library = m < 0 ? null : BridgeFiles.readLibrary(d);
            }
        } else {
            library = null;
        }
        scanned = true;
    }

    public boolean apply(LibrarySkin s) {
        if (busy() || availability() != Availability.READY) return false;
        Path d = vibeDir();
        if (d == null) return false;
        String id = UUID.randomUUID().toString();
        requestId = id;
        requestSkin = s.id();
        sentAt = System.currentTimeMillis();
        workingAt = 0;
        failure = null;
        phase = Phase.SENT;
        SkinRequest r = new SkinRequest(id, s.id(), s.slim(), sentAt / 1000);
        IO.execute(() -> {
            try {
                BridgeFiles.writeRequest(d, r);
            } catch (IOException e) {
                Vibey.LOG.warn("Skins: could not write the request", e);
                fail("io", null);
            }
        });
        return true;
    }

    void tick() {
        if (!busy()) return;
        if (++ticks % 5 != 0 || polling) return;
        long now = System.currentTimeMillis();
        if (phase == Phase.SENT && now - sentAt > ANSWER_TIMEOUT_MS) {
            withdraw();
            fail("no_answer", null);
            return;
        }
        if (phase == Phase.WORKING && now - workingAt > WORK_TIMEOUT_MS) {
            fail("timeout", null);
            return;
        }
        polling = true;
        String id = requestId;
        IO.execute(() -> {
            try {
                poll(id);
            } finally {
                polling = false;
            }
        });
    }

    private void poll(String id) {
        Path d = vibeDir();
        if (d == null || id == null || !id.equals(requestId)) return;
        SkinResult r = BridgeFiles.readResult(d);
        if (r == null || !id.equals(r.id())) return;
        switch (r.status()) {
            case WORKING -> {
                if (phase == Phase.SENT) {
                    workingAt = System.currentTimeMillis();
                    phase = Phase.WORKING;
                }
            }
            case ERROR -> fail(r.error(), r.message());
            case OK -> done(d, r);
        }
    }

    private void done(Path d, SkinResult r) {
        SkinLibrary l = library;
        String skinId = r.skinId() != null ? r.skinId() : requestSkin;
        LibrarySkin s = l == null ? null : l.find(skinId);
        boolean slim = r.skinId() != null ? r.slim() : s != null && s.slim();
        byte[] png = s == null ? null : BridgeFiles.readPng(d, s);
        if (l != null) library = l.withActive(skinId, slim);
        phase = Phase.DONE;
        Tasks.onMain(() -> {
            boolean shown = png != null && LocalSkin.set(localUuid(), png, slim);
            if (!shown) Vibey.LOG.info("Skins: changed, but no local copy to show until the next start");
            Notify.show(new Notify.Toast(Lang.tr("skins.toast.done"), Lang.tr("skins.toast.done.body"), Notify.Kind.SUCCESS, null, null));
        });
    }

    private void withdraw() {
        Path d = vibeDir();
        if (d == null) return;
        IO.execute(() -> {
            try {
                Files.deleteIfExists(d.resolve(BridgeFiles.REQUEST));
            } catch (IOException ignored) {
            }
        });
    }

    private void fail(String code, String message) {
        String text = message(code, message);
        failure = text;
        phase = Phase.FAILED;
        Notify.show(new Notify.Toast(Lang.tr("skins.toast.failed"), text, Notify.Kind.WARN, null, null));
    }

    static String message(String code, String launcherMessage) {
        String c = code == null ? "" : code;
        return switch (c) {
            case "no_answer", "timeout", "offline_account", "no_account", "signed_out", "not_found", "rate_limited",
                 "network", "mojang", "busy", "io" -> Lang.tr("skins.error." + c);
            default -> launcherMessage != null ? launcherMessage : Lang.tr("skins.error.other");
        };
    }

    void reset() {
        if (busy()) withdraw();
        phase = Phase.IDLE;
        failure = null;
    }
}

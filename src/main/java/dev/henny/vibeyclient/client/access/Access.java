package dev.henny.vibeyclient.client.access;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.access.AccessCache;
import dev.henny.vibeyclient.access.AccessGate;
import dev.henny.vibeyclient.access.AccessView;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.backend.SharedBackend;

public final class Access {
    private Access() {}

    private static AccessGate gate;
    private static boolean started;
    private static AccessView applied;

    public static boolean open() {
        if (AccessBypass.active()) return true;
        AccessGate g = gate;
        return g != null && g.open();
    }

    public static AccessView view() {
        AccessGate g = gate;
        return g == null ? AccessView.START : g.view();
    }

    public static void tick() {
        if (AccessBypass.active()) return;
        if (!started) start();
        AccessView now = gate.view();
        if (now.equals(applied)) return;
        AccessView before = applied;
        applied = now;
        AccessEffects.changed(before, now);
    }

    public static void recheck() {
        AccessGate g = gate;
        if (g == null || g.view().checking()) return;
        if (!GlobalSettings.online()) GlobalSettings.onlineFeatures.set(true);
        g.check(true);
    }

    private static void start() {
        started = true;
        BackendClient backend = SharedBackend.get();
        AccessCache cache = new AccessCache(Vibey.platform().gameDir().resolve("vibeyclient").resolve("access.json"));
        gate = new AccessGate(new BackendAccessSource(backend), cache, System::currentTimeMillis);
        backend.onAccessDenied(gate::denied);
        gate.check(false);
    }
}

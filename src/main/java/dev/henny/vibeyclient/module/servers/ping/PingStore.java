package dev.henny.vibeyclient.module.servers.ping;

import dev.henny.vibeyclient.module.servers.core.PingCache;
import net.minecraft.client.multiplayer.ServerData;

public final class PingStore {
    public static final int MAX_ENTRIES = 128;
    private static final PingCache<PingResult> CACHE = new PingCache<>(MAX_ENTRIES, 30_000);

    private PingStore() {}

    public static void ttl(int seconds) {
        CACHE.ttl(seconds * 1000L);
    }

    public static void remember(ServerData d) {
        if (d.state() == ServerData.State.SUCCESSFUL || d.state() == ServerData.State.INCOMPATIBLE) {
            CACHE.put(d.ip, PingResult.capture(d), System.nanoTime());
        }
    }

    public static boolean restore(ServerData d) {
        if (d.state() != ServerData.State.INITIAL) return false;
        PingResult r = CACHE.get(d.ip, System.nanoTime());
        if (r == null) return false;
        r.applyTo(d);
        return true;
    }

    public static void clear() {
        CACHE.clear();
    }
}

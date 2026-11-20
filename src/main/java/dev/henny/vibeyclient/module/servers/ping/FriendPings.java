package dev.henny.vibeyclient.module.servers.ping;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.servers.core.AddressKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.network.EventLoopGroupHolder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class FriendPings {
    static final long EVERY_NS = 30_000_000_000L, TIMEOUT_NS = LivePinger.TIMEOUT_MS * 1_000_000L;

    private record Probe(ServerData data, long started, boolean done, boolean ok) {}

    private final JoinMultiplayerScreen screen;
    private final Map<String, Probe> probes = new HashMap<>();
    private int version;

    public FriendPings(JoinMultiplayerScreen screen) {
        this.screen = screen;
    }

    public int version() {
        return version;
    }

    public void want(Collection<String> addresses) {
        long now = System.nanoTime();
        for (String a : addresses) {
            String k = AddressKey.of(a);
            Probe p = probes.get(k);
            if (p != null && !p.done() && now - p.started() > TIMEOUT_NS) finish(k, p.data(), false);
            p = probes.get(k);
            if (p == null || (p.done() && now - p.started() > EVERY_NS)) start(k, a, now);
        }
    }

    public String label(String address) {
        Probe p = address == null ? null : probes.get(AddressKey.of(address));
        if (p == null || !p.done()) return "";
        return p.ok() ? Lang.tr("servers.ui.ping", p.data().ping) : Lang.tr("servers.ui.offline");
    }

    public boolean bad(String address) {
        Probe p = address == null ? null : probes.get(AddressKey.of(address));
        return p != null && p.done() && !p.ok();
    }

    private void start(String key, String address, long now) {
        ServerData probe = new ServerData(address, address, ServerData.Type.OTHER);
        Probe old = probes.get(key);
        boolean keepOld = old != null && old.done();
        probes.put(key, keepOld ? new Probe(old.data(), now, true, old.ok()) : new Probe(probe, now, false, false));
        Minecraft mc = Minecraft.getInstance();
        EventLoopGroupHolder group = EventLoopGroupHolder.remote(mc.options.useNativeTransport());
        PingThreads.run(() -> {
            try {
                screen.getPinger().pingServer(probe, () -> {}, () -> mc.execute(() -> finish(key, probe, true)), group);
            } catch (Exception e) {
                mc.execute(() -> finish(key, probe, false));
            }
        });
    }

    private void finish(String key, ServerData probe, boolean ok) {
        Probe p = probes.get(key);
        if (p == null) return;
        boolean success = ok;
        probes.put(key, new Probe(probe, p.started(), true, success));
        version++;
    }
}

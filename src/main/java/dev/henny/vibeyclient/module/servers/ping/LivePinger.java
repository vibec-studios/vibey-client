package dev.henny.vibeyclient.module.servers.ping;

import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.module.servers.core.PingSchedule;
import dev.henny.vibeyclient.module.servers.mixin.OnlineServerEntryInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LivePinger {
    public static final long TIMEOUT_MS = 8000;

    private final JoinMultiplayerScreen screen;
    private final PingSchedule<ServerData> schedule = new PingSchedule<>(2, 30_000, TIMEOUT_MS);
    private final Map<ServerData, ServerData> probes = new IdentityHashMap<>();
    private final Set<ServerData> remembered = Collections.newSetFromMap(new IdentityHashMap<>());
    private ServerSelectionList list;
    private int started;

    public LivePinger(JoinMultiplayerScreen screen) {
        this.screen = screen;
    }

    public int started() {
        return started;
    }

    public void tick(ServersModule m, ServerSelectionList list) {
        this.list = list;
        if (!m.liveActive()) return;
        schedule.configure(m.pingParallel.get(), m.pingEvery.get() * 1000L);
        long now = System.nanoTime();
        for (ServerData d : schedule.expired(now)) finish(d, probes.get(d), false);
        List<PingSchedule.Candidate<ServerData>> visible = new ArrayList<>();
        for (ServerSelectionList.Entry e : list.children()) {
            if (!(e instanceof ServerSelectionList.OnlineServerEntry o)) continue;
            ServerData d = o.getServerData();
            boolean settled = settled(d);
            if (settled && remembered.add(d)) PingStore.remember(d);
            visible.add(new PingSchedule.Candidate<>(d, m.favourite(d.ip), settled));
        }
        for (ServerData d : schedule.due(visible, now)) start(d, now);
    }

    private static boolean settled(ServerData d) {
        ServerData.State s = d.state();
        return s == ServerData.State.SUCCESSFUL || s == ServerData.State.INCOMPATIBLE || s == ServerData.State.UNREACHABLE;
    }

    private void start(ServerData d, long now) {
        schedule.start(d, now);
        started++;
        ServerData probe = new ServerData(d.name, d.ip, ServerData.Type.OTHER);
        probes.put(d, probe);
        Minecraft mc = Minecraft.getInstance();
        EventLoopGroupHolder group = EventLoopGroupHolder.remote(mc.options.useNativeTransport());
        PingThreads.run(() -> {
            try {
                screen.getPinger().pingServer(probe, () -> {}, () -> mc.execute(() -> finish(d, probe, true)), group);
            } catch (Exception e) {
                mc.execute(() -> finish(d, probe, false));
            }
        });
    }

    private void finish(ServerData d, ServerData probe, boolean ok) {
        if (probes.get(d) != probe) return;
        probes.remove(d);
        schedule.done(d, System.nanoTime());
        if (probe == null) return;
        if (ok) {
            PingResult.capture(probe).applyTo(d);
            PingStore.remember(d);
        } else if (probe.state() == ServerData.State.INCOMPATIBLE) {
            PingResult.capture(probe).applyTo(d);
        } else {
            d.setState(ServerData.State.UNREACHABLE);
            d.motd = Component.translatable("multiplayer.status.cannot_connect").withColor(0xFF0000);
            d.status = CommonComponents.EMPTY;
            d.playerList = List.of();
        }
        refresh(d);
    }

    private void refresh(ServerData d) {
        if (list == null) return;
        for (ServerSelectionList.Entry e : list.children())
            if (e instanceof ServerSelectionList.OnlineServerEntry o && o.getServerData() == d) ((OnlineServerEntryInvoker) o).vibey$refreshStatus();
    }
}

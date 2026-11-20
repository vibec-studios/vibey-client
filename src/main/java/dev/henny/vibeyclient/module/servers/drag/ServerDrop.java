package dev.henny.vibeyclient.module.servers.drag;

import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.module.servers.core.DropSlot;
import dev.henny.vibeyclient.module.servers.core.ListMove;
import dev.henny.vibeyclient.module.servers.core.MoveTarget;
import dev.henny.vibeyclient.module.servers.screen.StarMark;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.List;

public final class ServerDrop implements DragTarget {
    static final int ICON = 32;

    private final ServersModule m;
    private final JoinMultiplayerScreen screen;
    private final ServerSelectionList list;

    public ServerDrop(ServersModule m, JoinMultiplayerScreen screen, ServerSelectionList list) {
        this.m = m;
        this.screen = screen;
        this.list = list;
    }

    @Override
    public boolean row(ObjectSelectionList.Entry<?> entry) {
        return entry instanceof ServerSelectionList.OnlineServerEntry;
    }

    @Override
    public boolean grabbable(ObjectSelectionList.Entry<?> entry, double x, double y) {
        ServerSelectionList.OnlineServerEntry e = (ServerSelectionList.OnlineServerEntry) entry;
        return x >= e.getContentX() + ICON && !StarMark.over(e, x, y);
    }

    @Override
    public boolean[] groups(List<ObjectSelectionList.Entry<?>> rows) {
        boolean[] g = new boolean[rows.size()];
        if (!m.favouritesActive() || !m.favouritesFirst.on()) return g;
        for (int i = 0; i < g.length; i++) g[i] = m.favourite(data(rows.get(i)).ip);
        return g;
    }

    @Override
    public void drop(List<ObjectSelectionList.Entry<?>> rows, int from, int slot) {
        int[] range = DropSlot.group(groups(rows), from);
        int target = DropSlot.clamp(slot, range[0], range[1]);
        if (DropSlot.noop(from, target)) return;
        ServerList servers = screen.getServers();
        int[] real = new int[rows.size()];
        for (int i = 0; i < real.length; i++) real[i] = indexOf(servers, data(rows.get(i)));
        for (int r : real) if (r < 0) return;
        ServerData moved = data(rows.get(from));
        ListMove.bySwaps(real[from], MoveTarget.finalIndex(real, from, target, range[0], range[1]), servers::swap);
        servers.save();
        list.updateOnlineServers(servers);
        reselect(moved);
    }

    private void reselect(ServerData moved) {
        for (ServerSelectionList.Entry e : list.children()) {
            if (e instanceof ServerSelectionList.OnlineServerEntry o && o.getServerData() == moved) list.setSelected(e);
        }
    }

    private static ServerData data(ObjectSelectionList.Entry<?> e) {
        return ((ServerSelectionList.OnlineServerEntry) e).getServerData();
    }

    private static int indexOf(ServerList servers, ServerData d) {
        for (int k = 0; k < servers.size(); k++) if (servers.get(k) == d) return k;
        return -1;
    }
}

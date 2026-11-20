package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.module.servers.core.ServerFilter;
import dev.henny.vibeyclient.module.servers.core.ServerOrder;
import dev.henny.vibeyclient.module.servers.ping.PingStore;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ServerListView {
    private static ServerFilter filter = ServerFilter.ALL;
    private static boolean reordered;

    private ServerListView() {}

    public static void filter(ServerFilter next) {
        filter = next;
    }

    public static boolean reordered() {
        return reordered;
    }

    public static void restore(ServerList list) {
        ServersModule m = ServersModule.get();
        if (m == null || !m.liveActive()) return;
        for (int i = 0; i < list.size(); i++) PingStore.restore(list.get(i));
    }

    public static Collection<ServerSelectionList.Entry> arrange(Collection<ServerSelectionList.Entry> entries) {
        ServersModule m = ServersModule.get();
        List<ServerSelectionList.Entry> all = new ArrayList<>(entries);
        if (m == null || !m.isEnabled()) {
            reordered = false;
            return entries;
        }
        List<ServerSelectionList.Entry> online = new ArrayList<>();
        List<ServerSelectionList.Entry> rest = new ArrayList<>();
        for (ServerSelectionList.Entry e : all) (e instanceof ServerSelectionList.OnlineServerEntry ? online : rest).add(e);
        boolean first = m.favouritesActive() && m.favouritesFirst.on();
        List<ServerSelectionList.Entry> shown = ServerOrder.arrange(online, ServerListView::keep, ServerListView::favourite, first);
        reordered = ServerOrder.reordered(online, shown);
        shown.addAll(rest);
        return shown;
    }

    private static boolean keep(ServerSelectionList.Entry e) {
        ServerData d = ((ServerSelectionList.OnlineServerEntry) e).getServerData();
        return filter.matches(d.name, d.ip, d.motd == null ? "" : d.motd.getString(), favourite(e));
    }

    private static boolean favourite(ServerSelectionList.Entry e) {
        ServersModule m = ServersModule.get();
        return m != null && m.favourite(((ServerSelectionList.OnlineServerEntry) e).getServerData().ip);
    }
}

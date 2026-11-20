package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.module.servers.mixin.JoinMultiplayerAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import java.util.List;

public final class ServerSwap {
    private ServerSwap() {}

    public static boolean swap(int i, int j) {
        if (!ServerListView.reordered() || !(Minecraft.getInstance().screen instanceof JoinMultiplayerScreen screen)) return false;
        ServerSelectionList list = ((JoinMultiplayerAccess) screen).vibey$list();
        List<ServerSelectionList.Entry> children = list.children();
        if (i < 0 || j < 0 || i >= children.size() || j >= children.size()) return true;
        if (!(children.get(i) instanceof ServerSelectionList.OnlineServerEntry a)) return true;
        if (!(children.get(j) instanceof ServerSelectionList.OnlineServerEntry b)) return true;
        ServerList servers = screen.getServers();
        int x = indexOf(servers, a.getServerData()), y = indexOf(servers, b.getServerData());
        if (x < 0 || y < 0) return true;
        servers.swap(x, y);
        servers.save();
        list.updateOnlineServers(servers);
        return true;
    }

    private static int indexOf(ServerList servers, ServerData d) {
        for (int k = 0; k < servers.size(); k++) if (servers.get(k) == d) return k;
        return -1;
    }
}

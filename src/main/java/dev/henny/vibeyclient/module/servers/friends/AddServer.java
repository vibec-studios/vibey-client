package dev.henny.vibeyclient.module.servers.friends;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.module.servers.core.AddressKey;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

final class AddServer {
    private AddServer() {}

    static boolean add(JoinMultiplayerScreen screen, ServerSelectionList list, String name, String address) {
        if (address == null || address.isBlank()) return false;
        ServerList servers = screen.getServers();
        String key = AddressKey.of(address);
        for (int i = 0; i < servers.size(); i++) {
            if (AddressKey.of(servers.get(i).ip).equals(key)) {
                Notify.info(Lang.tr("servers.friends.already"), servers.get(i).name);
                return false;
            }
        }
        servers.add(new ServerData(name == null || name.isBlank() ? address : name, address, ServerData.Type.OTHER), false);
        servers.save();
        if (list != null) list.updateOnlineServers(servers);
        Notify.success(Lang.tr("servers.friends.added"), name);
        return true;
    }
}

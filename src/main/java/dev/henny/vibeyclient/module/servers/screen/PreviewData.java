package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.servers.core.MotdSegment;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.List;

record PreviewData(String name, String ping, String players, String version, String sample, boolean bad,
                   boolean favourite, List<MotdSegment> motd) {
    static final int MAX_SAMPLE = 6;

    static PreviewData of(ServerData d, boolean favourite) {
        ServerData.State s = d.state();
        boolean bad = s == ServerData.State.UNREACHABLE || s == ServerData.State.INCOMPATIBLE;
        String ping = s == ServerData.State.SUCCESSFUL ? Lang.tr("servers.ui.ping", d.ping)
                : s == ServerData.State.UNREACHABLE ? Lang.tr("servers.ui.offline") : "";
        String players = d.players == null ? "" : Lang.tr("servers.ui.players", d.players.online(), d.players.max());
        String version = d.version == null ? "" : d.version.getString();
        return new PreviewData(d.name, ping, players, version, sample(d.playerList), bad, favourite, MotdReader.read(d.motd));
    }

    private static String sample(List<Component> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_SAMPLE, list.size()); i++) {
            if (i > 0) b.append(", ");
            b.append(list.get(i).getString());
        }
        return b.toString();
    }
}

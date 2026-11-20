package dev.henny.vibeyclient.module.servers.ping;

import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;

import java.util.List;

public record PingResult(Component motd, Component status, ServerStatus.Players players, long ping, Component version,
                         int protocol, List<Component> playerList) {
    public static PingResult capture(ServerData d) {
        return new PingResult(d.motd, d.status, d.players, d.ping, d.version, d.protocol, List.copyOf(d.playerList));
    }

    public void applyTo(ServerData d) {
        d.motd = motd;
        d.status = status;
        d.players = players;
        d.ping = ping;
        d.version = version;
        d.protocol = protocol;
        d.playerList = playerList;
        d.setState(compatible() ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
    }

    public boolean compatible() {
        return protocol == SharedConstants.getCurrentVersion().protocolVersion();
    }
}

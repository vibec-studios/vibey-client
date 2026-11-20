package dev.henny.vibeyclient.api.service;

import java.util.List;

public interface FriendsActivity {
    String READY = "ready", SIGNED_OUT = "signedOut", CONNECTING = "connecting", ERROR = "error";

    record Player(String id, String name, String skinUrl, String status, String rank) {}

    record OnServer(Player friend, String server, String address, long since, boolean lan) {
        public OnServer(Player friend, String server, String address, long since) {
            this(friend, server, address, since, false);
        }

        public boolean shared() {
            return !lan && address != null && !address.isBlank();
        }
    }

    record HostedWorld(Player host, String world, String address, int port, int players, long since) {
        public boolean joinable() {
            return address != null && !address.isBlank();
        }
    }

    record Invite(String id, Player from, String kind, String target, String host, int port, long expiresAt) {
        public String address() {
            if (host == null || host.isBlank()) return null;
            String h = host.indexOf(':') >= 0 && !host.startsWith("[") ? "[" + host + "]" : host;
            return port <= 0 || port == 25565 ? h : h + ":" + port;
        }
    }

    String state();

    int revision();

    List<OnServer> servers();

    List<HostedWorld> worlds();

    List<Invite> invites();

    AutoCloseable listen(Runnable onChange);

    void joinServer(String friendId);

    void joinWorld(String hostId);

    void accept(String inviteId);

    void decline(String inviteId);

    void signIn();

    void openFriends();
}

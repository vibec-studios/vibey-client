package dev.henny.vibeyclient.api.service;

import java.util.List;

public interface FriendsActivity {
    String READY = "ready", SIGNED_OUT = "signedOut", CONNECTING = "connecting", ERROR = "error";

    record Player(String id, String name, String skinUrl, String status, String rank) {}

    record OnServer(Player friend, String server, String address, long since) {
        public boolean shared() {
            return address != null && !address.isBlank();
        }
    }

    record HostedWorld(Player host, String world, String address, int port, int players, long since) {
        public boolean joinable() {
            return address != null && !address.isBlank();
        }
    }

    record Invite(String id, Player from, String kind, String target, long expiresAt) {}

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

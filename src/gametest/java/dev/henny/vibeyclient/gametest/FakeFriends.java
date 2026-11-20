package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.service.FriendsActivity;

import java.util.ArrayList;
import java.util.List;

final class FakeFriends implements FriendsActivity {
    String state = READY;
    int revision;
    List<OnServer> servers = new ArrayList<>();
    List<HostedWorld> worlds = new ArrayList<>();
    List<Invite> invites = new ArrayList<>();
    final List<String> calls = new ArrayList<>();

    static Player player(String id, String name, String status) {
        return new Player(id, name, null, status, null);
    }

    static FakeFriends full(String pingAddress) {
        long now = System.currentTimeMillis();
        FakeFriends f = new FakeFriends();
        f.servers.add(new OnServer(player(id('a'), "Mia", "online"), "Example SMP", pingAddress, now - 12 * 60_000));
        f.servers.add(new OnServer(player(id('b'), "Jonas", "busy"), null, null, now - 3 * 3_600_000));
        f.servers.add(new OnServer(player(id('e'), "Noah", "online"), "Secret SMP", null, now - 5 * 60_000));
        f.worlds.add(new HostedWorld(player(id('c'), "Lea", "online"), "Cherry Valley", "mossy-otter-27.axolite.cc", 25570, 3,
                now - 40 * 60_000));
        f.invites.add(new Invite("inv-1", player(id('d'), "Ben", "away"), "world", "Sky Farm", null, 0, now + 9 * 60_000));
        f.invites.add(new Invite("inv-2", player(id('f'), "Ida", "online"), "server", "Example SMP", "play.example.org", 25570,
                now + 20 * 60_000));
        return f;
    }

    static String id(char c) {
        return "00000000-0000-0000-0000-00000000000" + c;
    }

    void changed() {
        revision++;
    }

    @Override
    public String state() {
        return state;
    }

    @Override
    public int revision() {
        return revision;
    }

    @Override
    public List<OnServer> servers() {
        return servers;
    }

    @Override
    public List<HostedWorld> worlds() {
        return worlds;
    }

    @Override
    public List<Invite> invites() {
        return invites;
    }

    @Override
    public AutoCloseable listen(Runnable onChange) {
        return () -> {};
    }

    @Override
    public void joinServer(String friendId) {
        calls.add("joinServer:" + friendId);
    }

    @Override
    public void joinWorld(String hostId) {
        calls.add("joinWorld:" + hostId);
    }

    @Override
    public void accept(String inviteId) {
        calls.add("accept:" + inviteId);
    }

    @Override
    public void decline(String inviteId) {
        calls.add("decline:" + inviteId);
        invites.removeIf(i -> i.id().equals(inviteId));
        changed();
    }

    @Override
    public void signIn() {
        calls.add("signIn");
    }

    @Override
    public void openFriends() {
        calls.add("openFriends");
    }
}

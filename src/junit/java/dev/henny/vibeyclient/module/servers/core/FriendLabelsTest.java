package dev.henny.vibeyclient.module.servers.core;

import dev.henny.vibeyclient.api.service.FriendsActivity.HostedWorld;
import dev.henny.vibeyclient.api.service.FriendsActivity.Invite;
import dev.henny.vibeyclient.api.service.FriendsActivity.OnServer;
import dev.henny.vibeyclient.api.service.FriendsActivity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendLabelsTest {
    static final Player P = new Player("1", "Mia", null, "online", null);

    @Test
    void sharedServerShowsNameAndAddress() {
        FriendLabels.Label l = FriendLabels.server(new OnServer(P, "Example SMP", "play.example.org:25570", 0));
        assertEquals(new FriendLabels.Label("Example SMP", "play.example.org:25570"), l);
        assertEquals(new FriendLabels.Label("play.example.org", ""), FriendLabels.server(new OnServer(P, null, "play.example.org", 0)));
        assertEquals(new FriendLabels.Label("play.example.org", ""), FriendLabels.server(new OnServer(P, "play.example.org", "play.example.org", 0)));
    }

    @Test
    void hiddenServer() {
        FriendLabels.Label named = FriendLabels.server(new OnServer(P, "Example SMP", null, 0));
        assertEquals("Example SMP", named.title());
        assertTrue(named.hiddenDetail());
        FriendLabels.Label none = FriendLabels.server(new OnServer(P, null, null, 0));
        assertTrue(none.hiddenTitle());
        assertEquals("", none.detail());
    }

    @Test
    void lanServerHasNoAddress() {
        OnServer unnamed = new OnServer(P, null, "192.168.0.2", 0, true);
        assertTrue(!unnamed.shared());
        FriendLabels.Label none = FriendLabels.server(unnamed);
        assertTrue(none.hiddenTitle());
        assertEquals("", none.detail());
        FriendLabels.Label named = FriendLabels.server(new OnServer(P, "Basement", null, 0, true));
        assertEquals("Basement", named.title());
        assertTrue(named.hiddenDetail());
    }

    @Test
    void worldAndInvite() {
        assertEquals("Inselwelt", FriendLabels.world(new HostedWorld(P, " Inselwelt ", "x.axolite.cc", 25565, 1, 0)).title());
        assertTrue(FriendLabels.world(new HostedWorld(P, null, "x.axolite.cc", 25565, 1, 0)).hiddenTitle());
        Invite dest = new Invite("i", P, "server", "Example SMP", "play.example.org", 25570, 0);
        assertEquals(new FriendLabels.Label("Example SMP", "play.example.org:25570"), FriendLabels.invite(dest));
        Invite plain = new Invite("i", P, "world", "Farm", null, 0, 0);
        assertEquals(new FriendLabels.Label("Farm", ""), FriendLabels.invite(plain));
        Invite v6 = new Invite("i", P, "server", null, "2001:db8::1", 25565, 0);
        assertEquals(new FriendLabels.Label("[2001:db8::1]", ""), FriendLabels.invite(v6));
    }
}

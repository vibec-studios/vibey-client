package dev.henny.vibeyclient.module.servers.core;

import dev.henny.vibeyclient.api.service.FriendsActivity.HostedWorld;
import dev.henny.vibeyclient.api.service.FriendsActivity.Invite;
import dev.henny.vibeyclient.api.service.FriendsActivity.OnServer;
import dev.henny.vibeyclient.api.service.FriendsActivity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendSectionsTest {
    static Player p(String id, String name) {
        return new Player(id, name, null, "online", null);
    }

    @Test
    void sectionsInOrderWithHeadersAndCounts() {
        List<OnServer> servers = List.of(new OnServer(p("1", "Zoe"), "play.b.net", "play.b.net", 0),
                new OnServer(p("2", "Ann"), "a.net", null, 0));
        List<HostedWorld> worlds = List.of(new HostedWorld(p("3", "Kai"), "Island", "x.relay", 0, 2, 0),
                new HostedWorld(p("3", "Kai"), "Dup", "y.relay", 0, 1, 0));
        List<Invite> invites = List.of(new Invite("i1", p("4", "Max"), "world", "Farm", null, 0, 5_000),
                new Invite("old", p("5", "Old"), "world", "Gone", null, 0, 500));
        List<FriendSections.Row> rows = FriendSections.build(servers, worlds, invites, 1_000);
        List<String> keys = rows.stream().map(FriendSections.Row::key).toList();
        assertEquals(List.of("h:invites", "i:i1", "h:servers", "s:2", "s:1", "h:worlds", "w:3"), keys);
        assertEquals(2, rows.get(2).count());
        assertEquals(FriendSections.Kind.HEADER, rows.get(5).kind());
    }

    @Test
    void emptyInputGivesNoRows() {
        assertTrue(FriendSections.build(List.of(), List.of(), List.of(), 0).isEmpty());
        List<Invite> expired = List.of(new Invite("i", p("1", "A"), "world", "W", null, 0, 10));
        assertTrue(FriendSections.build(List.of(), List.of(), expired, 20).isEmpty());
    }
}

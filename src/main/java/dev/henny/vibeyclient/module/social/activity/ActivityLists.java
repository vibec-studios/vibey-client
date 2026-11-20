package dev.henny.vibeyclient.module.social.activity;

import dev.henny.vibeyclient.api.service.FriendsActivity.HostedWorld;
import dev.henny.vibeyclient.api.service.FriendsActivity.Invite;
import dev.henny.vibeyclient.api.service.FriendsActivity.OnServer;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Destination;
import dev.henny.vibeyclient.module.social.model.Model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ActivityLists {
    private ActivityLists() {}

    static List<OnServer> servers(SocialState st) {
        List<OnServer> out = new ArrayList<>();
        for (Model.Friend f : st.sortedFriends()) {
            Model.Presence p = f.presence();
            Model.Activity a = p == null || !p.online() ? null : p.activity();
            if (a == null || "hosting".equals(a.type()) || !SharedAddress.onServer(a)) continue;
            out.add(new OnServer(ActivityPlayers.of(f.user(), p), SharedAddress.title(a), SharedAddress.of(a), a.startedAt()));
        }
        return out;
    }

    static List<HostedWorld> worlds(SocialState st) {
        List<HostedWorld> out = new ArrayList<>();
        Set<String> hosts = new HashSet<>();
        for (Model.HostSession s : st.friendHosts.values()) {
            if (!s.online() || s.host() == null || !hosts.add(s.host().id())) continue;
            Model.Presence p = st.presenceOf(s.host().id());
            String world = HostedJoin.world(s, p == null ? null : p.activity());
            out.add(new HostedWorld(ActivityPlayers.of(s.host(), p), world, HostedJoin.host(s), HostedJoin.port(s), s.players(),
                    s.startedAt()));
        }
        for (Model.Friend f : st.sortedFriends()) {
            Model.Presence p = f.presence();
            Model.Activity a = p == null || !p.online() ? null : p.activity();
            if (a == null || !"hosting".equals(a.type()) || hosts.contains(f.user().id())) continue;
            Model.Join j = a.joinable() ? a.join() : null;
            hosts.add(f.user().id());
            out.add(new HostedWorld(ActivityPlayers.of(f.user(), p), a.world(), j == null ? null : j.address(),
                    j == null ? 0 : j.port(), -1, a.startedAt()));
        }
        return out;
    }

    static List<Invite> invites(SocialState st) {
        List<Invite> out = new ArrayList<>();
        for (Model.Invite i : st.openIncomingInvites()) {
            Destination d = i.destination();
            out.add(new Invite(i.id(), ActivityPlayers.of(i.from(), st.presenceOf(i.from().id())), i.kind(), i.displayName(),
                    d == null ? null : d.host(), d == null ? 0 : d.port(), i.expiresAt()));
        }
        return out;
    }
}

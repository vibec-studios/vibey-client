package dev.henny.vibeyclient.module.servers.friends;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.service.FriendsActivity.HostedWorld;
import dev.henny.vibeyclient.api.service.FriendsActivity.Invite;
import dev.henny.vibeyclient.api.service.FriendsActivity.OnServer;
import dev.henny.vibeyclient.api.service.FriendsActivity.Player;
import dev.henny.vibeyclient.client.ranks.Ranks;
import dev.henny.vibeyclient.module.servers.core.Durations;
import dev.henny.vibeyclient.module.servers.core.FriendSections;
import dev.henny.vibeyclient.module.servers.ping.FriendPings;
import dev.henny.vibeyclient.ranks.RankDef;
import dev.henny.vibeyclient.ui.vui.HeadTextures;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class FriendItems {
    private FriendItems() {}

    static List<FriendItem> of(List<FriendSections.Row> rows, FriendPings pings, long now) {
        List<FriendItem> out = new ArrayList<>(rows.size());
        for (FriendSections.Row r : rows) {
            out.add(switch (r.kind()) {
                case HEADER -> FriendItem.header(r.key(), Lang.tr("servers.friends.section." + r.section().name().toLowerCase(Locale.ROOT)), r.count());
                case SERVER -> server(r.key(), (OnServer) r.item(), pings, now);
                case WORLD -> world(r.key(), (HostedWorld) r.item(), now);
                case INVITE -> invite(r.key(), (Invite) r.item(), now);
            });
        }
        return out;
    }

    private static FriendItem server(String key, OnServer s, FriendPings pings, long now) {
        Player p = s.friend();
        String ping = s.shared() ? pings.label(s.address()) : "";
        String where = s.shared() ? s.server() : Lang.tr("servers.friends.hidden");
        String card = join(Lang.tr("servers.friends.card.server", where), since(s.since(), now));
        return new FriendItem(key, "server", "", 0, p.id(), p.name(), HeadTextures.of(p.id(), p.skinUrl()), rank(p),
                rankColor(p), "server", where, ping, pings.bad(s.address()), s.shared(), s.shared(), false,
                s.shared() ? s.address() : "", p.status(), card);
    }

    private static FriendItem world(String key, HostedWorld w, long now) {
        Player p = w.host();
        String name = w.world() == null || w.world().isBlank() ? Lang.tr("servers.friends.world") : w.world();
        String players = w.players() > 0 ? Lang.tr("servers.friends.players", w.players()) : "";
        String card = join(Lang.tr("servers.friends.card.world", name), since(w.since(), now));
        return new FriendItem(key, "world", "", 0, p.id(), p.name(), HeadTextures.of(p.id(), p.skinUrl()), rank(p),
                rankColor(p), "host", name, players, false, w.joinable(), false, false, "", p.status(), card);
    }

    private static FriendItem invite(String key, Invite i, long now) {
        Player p = i.from();
        String what = i.target() == null || i.target().isBlank() ? Lang.tr("servers.friends.world") : i.target();
        Durations.Span left = Durations.left(i.expiresAt(), now);
        String expires = left == null ? "" : Lang.tr("servers.friends.expires", span(left));
        String card = join(Lang.tr("servers.friends.card.invite", what), expires);
        return new FriendItem(key, "invite", "", 0, i.id(), p.name(), HeadTextures.of(p.id(), p.skinUrl()), rank(p),
                rankColor(p), "world".equals(i.kind()) ? "host" : "server", what, expires, false, true, false, true, "",
                p.status(), card);
    }

    private static String since(long startedAt, long now) {
        Durations.Span s = Durations.since(startedAt, now);
        return s == null ? "" : Lang.tr("servers.friends.since", span(s));
    }

    static String span(Durations.Span s) {
        return Lang.tr("servers.friends.unit." + s.unit().name().toLowerCase(Locale.ROOT), s.amount());
    }

    private static String join(String a, String b) {
        if (a.isEmpty()) return b;
        return b.isEmpty() ? a : a + " · " + b;
    }

    private static String rank(Player p) {
        RankDef def = p.rank() == null ? null : Ranks.rank(p.rank());
        return def == null ? "" : def.label().get(Lang.german());
    }

    private static String rankColor(Player p) {
        RankDef def = p.rank() == null ? null : Ranks.rank(p.rank());
        return def == null ? "" : String.format(Locale.ROOT, "#%06x", def.color() & 0xFFFFFF);
    }
}

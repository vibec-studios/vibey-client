package dev.henny.vibeyclient.module.servers.core;

import dev.henny.vibeyclient.api.service.FriendsActivity.HostedWorld;
import dev.henny.vibeyclient.api.service.FriendsActivity.Invite;
import dev.henny.vibeyclient.api.service.FriendsActivity.OnServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FriendSections {
    public enum Kind { HEADER, INVITE, SERVER, WORLD }

    public enum Section { INVITES, SERVERS, WORLDS }

    public record Row(String key, Kind kind, Section section, int count, Object item) {}

    private FriendSections() {}

    public static List<Row> build(List<OnServer> servers, List<HostedWorld> worlds, List<Invite> invites, long now) {
        List<Row> out = new ArrayList<>();
        List<Invite> open = new ArrayList<>();
        for (Invite i : invites) if (i.expiresAt() <= 0 || i.expiresAt() > now) open.add(i);
        open.sort(Comparator.comparingLong(i -> i.expiresAt() <= 0 ? Long.MAX_VALUE : i.expiresAt()));
        section(out, Section.INVITES, Kind.INVITE, open, i -> "i:" + i.id());
        List<OnServer> playing = new ArrayList<>(servers);
        playing.sort(Comparator.comparing((OnServer s) -> lower(s.server())).thenComparing(s -> lower(s.friend().name())));
        section(out, Section.SERVERS, Kind.SERVER, playing, s -> "s:" + s.friend().id());
        List<HostedWorld> hosted = unique(worlds);
        hosted.sort(Comparator.comparing(w -> lower(w.host().name())));
        section(out, Section.WORLDS, Kind.WORLD, hosted, w -> "w:" + w.host().id());
        return out;
    }


    private static <T> void section(List<Row> out, Section section, Kind kind, List<T> items, java.util.function.Function<T, String> key) {
        if (items.isEmpty()) return;
        out.add(new Row("h:" + section.name().toLowerCase(Locale.ROOT), Kind.HEADER, section, items.size(), null));
        for (T t : items) out.add(new Row(key.apply(t), kind, section, 0, t));
    }

    private static List<HostedWorld> unique(List<HostedWorld> worlds) {
        Set<String> seen = new HashSet<>();
        List<HostedWorld> out = new ArrayList<>();
        for (HostedWorld w : worlds) if (seen.add(w.host().id())) out.add(w);
        return out;
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}

package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.client.VibeyPlayers;
import dev.henny.vibeyclient.client.ranks.Ranks;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class DecorCache {
    private record Entry(Component in, long players, long ranks, int flags, long bucket, Component out) {
        boolean matches(Component name, long p, long r, int f, long b) {
            return players == p && ranks == r && flags == f && bucket == b && (in == name || in.equals(name));
        }
    }

    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    Component get(UUID player, String profileName, Component name, int flags, long bucket) {
        long p = VibeyPlayers.version(), r = Ranks.version();
        Entry e = entries.get(player);
        if (e != null && e.matches(name, p, r, flags, bucket)) return e.out;
        Component out = Decorator.build(player, profileName, name, flags, bucket);
        if (entries.size() > 512) entries.clear();
        entries.put(player, new Entry(name, p, r, flags, bucket, out));
        return out;
    }

    void clear() {
        entries.clear();
    }
}

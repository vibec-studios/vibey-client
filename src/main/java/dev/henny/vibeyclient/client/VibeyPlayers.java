package dev.henny.vibeyclient.client;

import dev.henny.vibeyclient.client.ranks.Ranks;
import dev.henny.vibeyclient.ranks.Identity;
import dev.henny.vibeyclient.ranks.LegacyBadges;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VibeyPlayers {
    private VibeyPlayers() {}

    public static final List<String> KNOWN_BADGES = List.of("staff", "creator", "supporter");

    private record Backend(boolean vibey, List<String> badges, Identity identity) {}

    private static final Map<UUID, Backend> BACKEND = new ConcurrentHashMap<>();
    private static final Set<UUID> SERVER = ConcurrentHashMap.newKeySet();
    private static volatile long version;

    public static void backend(UUID id, boolean vibey, List<String> badges) {
        backend(id, vibey, badges, LegacyBadges.identity(badges == null ? List.of() : badges));
    }

    public static void backend(UUID id, boolean vibey, List<String> badges, Identity identity) {
        if (id == null) return;
        List<String> clean = badges == null || badges.isEmpty() ? List.of() : badges.stream()
                .map(b -> b == null ? "" : b.toLowerCase(Locale.ROOT))
                .filter(KNOWN_BADGES::contains)
                .sorted(java.util.Comparator.comparingInt(KNOWN_BADGES::indexOf))
                .distinct().toList();
        Backend n = new Backend(vibey, clean, identity == null ? Identity.EMPTY : identity);
        if (!n.equals(BACKEND.put(id, n))) version++;
    }

    public static void server(UUID id) {
        if (id != null && SERVER.add(id)) version++;
    }

    public static void clearServer() {
        if (!SERVER.isEmpty()) {
            SERVER.clear();
            version++;
        }
    }

    public static void clearBackend() {
        if (!BACKEND.isEmpty()) {
            BACKEND.clear();
            version++;
        }
    }

    public static boolean isVibey(UUID id) {
        if (id == null) return false;
        if (id.equals(self())) return true;
        if (SERVER.contains(id)) return true;
        Backend b = BACKEND.get(id);
        return b != null && b.vibey;
    }

    public static List<String> badges(UUID id) {
        Backend b = id == null ? null : BACKEND.get(id);
        return b == null ? List.of() : b.badges;
    }

    public static Identity identity(UUID id) {
        Backend b = id == null ? null : BACKEND.get(id);
        if (b == null) return Identity.EMPTY;
        if (!Ranks.own().showRank() && id.equals(self())) return b.identity.withoutRanks();
        return b.identity;
    }

    public static long version() {
        return version;
    }

    public static boolean isSelf(UUID id) {
        return id != null && id.equals(self());
    }

    private static UUID self() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        if (mc.player != null) return mc.player.getUUID();
        return mc.getUser() == null ? null : mc.getUser().getProfileId();
    }
}

package dev.henny.vibeyclient.client;

import dev.henny.vibeyclient.Vibey;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which players around us run Vibey Client and which badges they have (VIBE-API-2 §5). Fed by the cosmetics lookup
 * ({@code client: "vibey"}, {@code badges}) and by the server channel (every player whose {@code vibeyclient:*}
 * packets reach us said hello, so has the mod). We always count ourselves. Thread safe; written on the client thread.
 */
public final class VibeyPlayers {
    private VibeyPlayers() {}

    /** Badge ids the client has an icon for, in display order. Unknown ids are ignored (logged once). */
    public static final List<String> KNOWN_BADGES = List.of("staff", "creator", "supporter");

    private record Backend(boolean vibey, List<String> badges) {}

    private static final Map<UUID, Backend> BACKEND = new ConcurrentHashMap<>();
    private static final Set<UUID> SERVER = ConcurrentHashMap.newKeySet();
    private static final Set<String> UNKNOWN_LOGGED = ConcurrentHashMap.newKeySet();
    private static volatile long version;

    /** Lookup answer for one player ({@code client} null/absent = not known to run Vibey right now). */
    public static void backend(UUID id, boolean vibey, List<String> badges) {
        List<String> clean = badges == null || badges.isEmpty() ? List.of() : badges.stream()
                .map(b -> b == null ? "" : b.toLowerCase(Locale.ROOT))
                .filter(b -> {
                    if (KNOWN_BADGES.contains(b)) return true;
                    if (!b.isEmpty() && UNKNOWN_LOGGED.add(b)) Vibey.LOG.info("Unknown player badge '{}' ignored", b);
                    return false;
                })
                .sorted(java.util.Comparator.comparingInt(KNOWN_BADGES::indexOf))
                .distinct().toList();
        Backend n = new Backend(vibey, clean);
        if (!n.equals(BACKEND.put(id, n))) version++;
    }

    /** A {@code vibeyclient:*} packet from this player reached us through the server. */
    public static void server(UUID id) {
        if (id != null && SERVER.add(id)) version++;
    }

    /** Left the world: server-channel knowledge is per server. */
    public static void clearServer() {
        if (!SERVER.isEmpty()) {
            SERVER.clear();
            version++;
        }
    }

    /** Backend went away (online features off, logged out). */
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

    /** Known badges of the player (display order), never null. */
    public static List<String> badges(UUID id) {
        Backend b = id == null ? null : BACKEND.get(id);
        return b == null ? List.of() : b.badges;
    }

    /** Increases on every change (for caches). */
    public static long version() {
        return version;
    }

    private static UUID self() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        if (mc.player != null) return mc.player.getUUID();
        return mc.getUser() == null ? null : mc.getUser().getProfileId();
    }
}

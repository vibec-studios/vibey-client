package dev.henny.cosmetico.state;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class AvatarRegistry {
    private final Map<UUID, Avatar> avatars = new HashMap<>();

    public Avatar get(UUID uuid) {
        return avatars.computeIfAbsent(uuid, u -> new Avatar(u, false));
    }

    public Avatar peek(UUID uuid) {
        return avatars.get(uuid);
    }

    public Collection<Avatar> all() {
        return avatars.values();
    }

    public void remove(UUID uuid) {
        avatars.remove(uuid);
    }

    public void clear() {
        avatars.clear();
    }

    public void prune(long tick, long maxAge) {
        for (Iterator<Avatar> it = avatars.values().iterator(); it.hasNext(); ) {
            Avatar a = it.next();
            if (tick - a.lastSeenTick > maxAge && a.emote == null) it.remove();
        }
    }
}

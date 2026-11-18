package dev.henny.vibeyclient.avatar;

import dev.henny.vibeyclient.avatar.anim.Clock;
import dev.henny.vibeyclient.avatar.anim.EmotePlay;
import dev.henny.vibeyclient.avatar.bundle.Bundle;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The {@link AvatarState} of every player we know about, by UUID. Loadouts come from the sync code (own loadout,
 * backend, server packets); the render hooks read them. Client thread only.
 */
public final class AvatarStates {
    private AvatarStates() {}

    private static final Map<UUID, AvatarState> STATES = new HashMap<>();
    /** Called when an emote ends on an avatar (uuid). */
    public static Consumer<AvatarState> onEmoteEnded = s -> {};

    public static AvatarState get(UUID uuid) {
        return STATES.computeIfAbsent(uuid, u -> new AvatarState(u, false));
    }

    public static AvatarState peek(UUID uuid) {
        return STATES.get(uuid);
    }

    public static Collection<AvatarState> all() {
        return STATES.values();
    }

    public static void remove(UUID uuid) {
        STATES.remove(uuid);
    }

    public static void clear() {
        STATES.clear();
    }

    /** Replaces the slots of {@code uuid} ({@code slot id → item id}). */
    public static void setLoadout(UUID uuid, Map<String, String> slots) {
        AvatarState s = get(uuid);
        java.util.Arrays.fill(s.slots, null);
        for (Map.Entry<String, String> e : slots.entrySet()) {
            Slot slot = Slot.of(e.getKey());
            if (slot != null && e.getValue() != null && !e.getValue().isEmpty()) s.slots[slot.ordinal()] = e.getValue();
        }
    }

    /** Starts an emote on {@code state} (replacing one that plays). Returns false if the emote is unknown/not loaded. */
    public static boolean play(AvatarState state, String emoteId) {
        AvatarItem item = AvatarItems.get(emoteId);
        Bundle b = item == null ? null : item.bundle();
        if (b == null || !b.isEmote() || b.emoteAnimation() == null) return false;
        double now = Clock.now();
        state.emote = new EmotePlay(emoteId, b, now);
        state.emote.update(now);
        return true;
    }

    public static void stop(AvatarState state) {
        if (state.emote != null) state.emote.stop(Clock.now());
    }

    /** Hot reload: an emote bundle was replaced; restart plays of it with the new bundle. */
    public static void reloadEmote(String id) {
        for (AvatarState s : STATES.values()) {
            if (s.emote != null && s.emote.id.equals(id) && !s.emote.stopping()) play(s, id);
        }
    }

    /** Advances emotes (fade, end). Called every client tick and by previews. */
    public static void tickEmote(AvatarState s, double now) {
        EmotePlay e = s.emote;
        if (e == null) return;
        e.update(now);
        if (e.finished(now)) {
            s.emote = null;
            onEmoteEnded.accept(s);
        }
    }

    /** Drops states of players not seen for a while (left the area). */
    public static void prune(long tick, long maxAge) {
        for (Iterator<AvatarState> it = STATES.values().iterator(); it.hasNext(); ) {
            AvatarState s = it.next();
            if (tick - s.lastSeenTick > maxAge && s.emote == null) it.remove();
        }
    }
}

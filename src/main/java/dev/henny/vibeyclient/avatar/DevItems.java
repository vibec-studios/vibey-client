package dev.henny.vibeyclient.avatar;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * Dev items from the Blockbench live link (SPEC §8). Implemented by the engine and registered as a service
 * ({@code Services.get(DevItems.class)}); used by the devlink module. Call on the client thread only.
 */
public interface DevItems {
    enum Action { EQUIP, PLAY, NONE }

    /** Result of a push: ok + validator warnings, or an error message (bundle rejected, nothing changed). */
    record PushResult(String id, boolean ok, List<String> warnings, String error) {}

    /**
     * Parses and validates {@code bundleBytes} (the UTF-8 .vibey JSON), registers it as a DEV item (hot-replacing a
     * previous version with the same id, keeping its equip/emote state) and then equips (cosmetic) or plays (emote) it.
     */
    PushResult push(byte[] bundleBytes, Action action);

    /** Same as {@link #push(byte[], Action)} for an already parsed JSON object. */
    PushResult push(JsonObject bundle, Action action);

    /** Removes a dev item (unequips / stops it). Returns false if there was none. */
    boolean remove(String id);

    /** Ids of all dev items currently loaded. */
    List<String> ids();
}

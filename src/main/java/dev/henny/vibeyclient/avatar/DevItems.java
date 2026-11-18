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

    /**
     * Push with a wardrobe thumbnail (PNG, at most {@link #THUMBNAIL_SIZE}² px and {@link #THUMBNAIL_BYTES}; null =
     * none: a re-push of the same id keeps the thumbnail it had). An invalid thumbnail is ignored with a warning.
     */
    default PushResult push(JsonObject bundle, Action action, byte[] thumbnail) {
        return push(bundle, action);
    }

    /** {@link #push(JsonObject, Action, byte[])} for the raw bundle bytes. */
    default PushResult push(byte[] bundleBytes, Action action, byte[] thumbnail) {
        return push(bundleBytes, action);
    }

    /** Limits of a pushed thumbnail (SPEC §8). */
    int THUMBNAIL_SIZE = 256, THUMBNAIL_BYTES = 256 * 1024;

    /** Checks a pushed thumbnail: null if it is a PNG within the limits, otherwise the reason. */
    static String checkThumbnail(byte[] png) {
        if (png == null || png.length < 24) return "not a PNG";
        if (png.length > THUMBNAIL_BYTES) return "larger than 256 KiB";
        int[] sig = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        for (int i = 0; i < sig.length; i++) if ((png[i] & 0xFF) != sig[i]) return "not a PNG";
        if (png[12] != 0x49 || png[13] != 0x48 || png[14] != 0x44 || png[15] != 0x52) return "not a PNG";
        long w = ((png[16] & 0xFFL) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
        long h = ((png[20] & 0xFFL) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
        if (w < 1 || h < 1 || w > THUMBNAIL_SIZE || h > THUMBNAIL_SIZE) return "must be at most 256x256 px (is " + w + "x" + h + ")";
        return null;
    }

    /** Removes a dev item (unequips / stops it). Returns false if there was none. */
    boolean remove(String id);

    /** Ids of all dev items currently loaded. */
    List<String> ids();
}

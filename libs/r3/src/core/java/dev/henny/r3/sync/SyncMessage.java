package dev.henny.r3.sync;

import com.google.gson.JsonObject;
import dev.henny.r3.persist.PlacementRecord;

public record SyncMessage(SyncOp op, String kind, String objectId, long revision, PlacementRecord placement, JsonObject payload) {
    public static SyncMessage upsert(String kind, PlacementRecord placement) {
        return new SyncMessage(SyncOp.UPSERT, kind, placement.id, placement.revision, placement, null);
    }

    public static SyncMessage state(String kind, String id, long revision, JsonObject payload) {
        return new SyncMessage(SyncOp.STATE, kind, id, revision, null, payload);
    }

    public static SyncMessage remove(String kind, String id, long revision) {
        return new SyncMessage(SyncOp.REMOVE, kind, id, revision, null, null);
    }

    public static SyncMessage request(String kind) {
        return new SyncMessage(SyncOp.REQUEST, kind, "", 0, null, null);
    }
}

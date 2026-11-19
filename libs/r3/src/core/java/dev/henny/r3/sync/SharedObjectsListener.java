package dev.henny.r3.sync;

import com.google.gson.JsonObject;
import dev.henny.r3.persist.PlacementRecord;

import java.util.UUID;

public interface SharedObjectsListener {
    void upserted(UUID owner, PlacementRecord record, boolean created);

    default void state(UUID owner, String id, JsonObject payload) {}

    void removed(UUID owner, String id);
}

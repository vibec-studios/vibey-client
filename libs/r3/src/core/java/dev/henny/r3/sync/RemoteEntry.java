package dev.henny.r3.sync;

import dev.henny.r3.persist.PlacementRecord;

import java.util.UUID;

public final class RemoteEntry {
    private final UUID owner;
    private PlacementRecord record;
    private long revision;

    RemoteEntry(UUID owner, PlacementRecord record, long revision) {
        this.owner = owner;
        this.record = record;
        this.revision = revision;
    }

    public UUID owner() {
        return owner;
    }

    public PlacementRecord record() {
        return record;
    }

    public long revision() {
        return revision;
    }

    void update(PlacementRecord record, long revision) {
        if (record != null) this.record = record;
        this.revision = revision;
    }
}

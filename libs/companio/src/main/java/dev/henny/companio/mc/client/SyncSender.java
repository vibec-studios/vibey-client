package dev.henny.companio.mc.client;

import dev.henny.companio.pack.Pack;
import dev.henny.companio.pack.PackRegistry;
import dev.henny.companio.sync.Snapshot;
import dev.henny.companio.sync.SnapshotCodec;
import dev.henny.companio.sync.Snapshots;
import dev.henny.companio.sync.SyncPort;
import dev.henny.companio.world.Companion;

final class SyncSender {
    private static final int MIN_TICKS = 5, HEARTBEAT_TICKS = 60;

    private Snapshot last;
    private int since;
    private boolean announced;

    void tick(Companion c, PackRegistry registry, String dimension, SyncPort port, boolean share) {
        since++;
        if (c == null || !share) {
            if (announced) {
                port.send(SnapshotCodec.GONE);
                announced = false;
                last = null;
            }
            return;
        }
        if (since < MIN_TICKS) return;
        Pack p = registry.pack(c.def.packId());
        if (p == null) return;
        Snapshot s = Snapshots.of(c, p.hash(), dimension);
        if (!Snapshots.moved(last, s) && since < HEARTBEAT_TICKS) return;
        port.send(SnapshotCodec.encode(s));
        last = s;
        since = 0;
        announced = true;
    }

    void reset() {
        last = null;
        announced = false;
    }
}

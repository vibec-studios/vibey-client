package dev.henny.r3.sync;

import dev.henny.r3.persist.PlacementRecord;

import java.util.function.Predicate;

public record SyncPolicy(int maxObjectsPerOwner, double burst, double messagesPerSecond,
                         long answerDelayMinMs, long answerDelayMaxMs, Predicate<PlacementRecord> accept) {
    public static SyncPolicy defaults() {
        return new SyncPolicy(8, 6, 3, 300, 1000, r -> true);
    }

    public SyncPolicy accepting(Predicate<PlacementRecord> filter) {
        return new SyncPolicy(maxObjectsPerOwner, burst, messagesPerSecond, answerDelayMinMs, answerDelayMaxMs, filter);
    }

    public SyncPolicy maxPerOwner(int max) {
        return new SyncPolicy(max, burst, messagesPerSecond, answerDelayMinMs, answerDelayMaxMs, accept);
    }
}

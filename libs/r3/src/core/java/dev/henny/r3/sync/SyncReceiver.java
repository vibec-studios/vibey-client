package dev.henny.r3.sync;

import java.util.UUID;

@FunctionalInterface
public interface SyncReceiver {
    void receive(UUID sender, SyncMessage message);
}

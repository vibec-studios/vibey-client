package dev.henny.r3.sync;

import java.util.UUID;

public interface SyncChannel {
    boolean available();

    void broadcast(SyncMessage message);

    void sendTo(UUID player, SyncMessage message);

    void receiver(SyncReceiver receiver);
}

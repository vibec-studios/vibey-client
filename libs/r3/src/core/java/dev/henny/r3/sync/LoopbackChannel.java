package dev.henny.r3.sync;

import java.util.UUID;

public final class LoopbackChannel implements SyncChannel {
    private final LoopbackHub hub;
    private final UUID player;
    private SyncReceiver receiver;

    LoopbackChannel(LoopbackHub hub, UUID player) {
        this.hub = hub;
        this.player = player;
    }

    public static LoopbackChannel singleplayer(UUID player) {
        return new LoopbackHub().join(player);
    }

    public UUID player() {
        return player;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public void broadcast(SyncMessage message) {
        hub.broadcast(player, message);
    }

    @Override
    public void sendTo(UUID target, SyncMessage message) {
        hub.sendTo(player, target, message);
    }

    @Override
    public void receiver(SyncReceiver receiver) {
        this.receiver = receiver;
    }

    void deliver(UUID sender, String json) {
        if (receiver == null) return;
        try {
            receiver.receive(sender, SyncCodec.decode(json));
        } catch (IllegalArgumentException dropped) {
            return;
        }
    }
}

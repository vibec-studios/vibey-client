package dev.henny.r3.sync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class LoopbackNetwork {
    final Map<UUID, Endpoint> endpoints = new LinkedHashMap<>();
    final List<String> wire = new ArrayList<>();

    final class Endpoint implements SyncChannel {
        final UUID player;
        SyncReceiver receiver;

        Endpoint(UUID player) {
            this.player = player;
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public void broadcast(SyncMessage message) {
            String json = SyncCodec.encode(message);
            wire.add(json);
            for (Endpoint e : endpoints.values()) if (e != this && e.receiver != null) e.receiver.receive(player, SyncCodec.decode(json));
        }

        @Override
        public void sendTo(UUID target, SyncMessage message) {
            String json = SyncCodec.encode(message);
            wire.add(json);
            Endpoint e = endpoints.get(target);
            if (e != null && e.receiver != null) e.receiver.receive(player, SyncCodec.decode(json));
        }

        @Override
        public void receiver(SyncReceiver receiver) {
            this.receiver = receiver;
        }
    }

    Endpoint join(UUID player) {
        Endpoint e = new Endpoint(player);
        endpoints.put(player, e);
        return e;
    }
}

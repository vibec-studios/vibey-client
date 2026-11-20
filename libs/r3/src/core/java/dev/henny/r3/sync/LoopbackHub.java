package dev.henny.r3.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LoopbackHub {
    private final Map<UUID, LoopbackChannel> endpoints = new LinkedHashMap<>();
    private final List<String> wire = new ArrayList<>();

    public LoopbackChannel join(UUID player) {
        LoopbackChannel channel = new LoopbackChannel(this, player);
        endpoints.put(player, channel);
        return channel;
    }

    public void leave(UUID player) {
        endpoints.remove(player);
    }

    public List<String> wire() {
        return wire;
    }

    public Collection<LoopbackChannel> endpoints() {
        return endpoints.values();
    }

    void broadcast(UUID sender, SyncMessage message) {
        String json = SyncCodec.encode(message);
        wire.add(json);
        for (LoopbackChannel c : new ArrayList<>(endpoints.values())) {
            if (!c.player().equals(sender)) c.deliver(sender, json);
        }
    }

    void sendTo(UUID sender, UUID target, SyncMessage message) {
        String json = SyncCodec.encode(message);
        wire.add(json);
        LoopbackChannel c = endpoints.get(target);
        if (c != null) c.deliver(sender, json);
    }
}

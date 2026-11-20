package dev.henny.r3.sync;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoopbackChannelTest {
    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();

    @Test
    void broadcastReachesEveryoneButTheSenderAsJson() {
        LoopbackHub hub = new LoopbackHub();
        LoopbackChannel a = hub.join(alice);
        LoopbackChannel b = hub.join(bob);
        List<UUID> atA = new ArrayList<>();
        List<UUID> atB = new ArrayList<>();
        a.receiver((sender, m) -> atA.add(sender));
        b.receiver((sender, m) -> atB.add(sender));
        a.broadcast(new SyncMessage(SyncOp.REMOVE, "test:screen", "s1", 7, null, null));
        assertEquals(List.of(), atA);
        assertEquals(List.of(alice), atB);
        assertTrue(hub.wire().get(0).contains("\"op\":\"REMOVE\""));
    }

    @Test
    void sendToReachesOnePlayerAndSingleplayerIsAvailable() {
        LoopbackHub hub = new LoopbackHub();
        LoopbackChannel a = hub.join(alice);
        LoopbackChannel b = hub.join(bob);
        List<String> got = new ArrayList<>();
        b.receiver((sender, m) -> got.add(m.objectId()));
        a.sendTo(bob, new SyncMessage(SyncOp.REQUEST, "test:screen", "s2", 1, null, null));
        assertEquals(List.of("s2"), got);
        assertTrue(LoopbackChannel.singleplayer(alice).available());
    }
}

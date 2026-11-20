package dev.henny.r3.sync;

import com.google.gson.JsonObject;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.persist.PlacementCodec;
import dev.henny.r3.persist.PlacementRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedObjectsTest {
    private static final String KIND = "vibey:video";
    private final long[] clock = {1_000_000};
    private final LoopbackNetwork net = new LoopbackNetwork();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final List<String> bobSaw = new ArrayList<>();
    private final SharedObjects aliceSide = node(alice, new ArrayList<>(), SyncPolicy.defaults());
    private final SharedObjects bobSide = node(bob, bobSaw, SyncPolicy.defaults());

    private SharedObjects node(UUID who, List<String> log, SyncPolicy policy) {
        SharedObjectsListener listener = new SharedObjectsListener() {
            @Override
            public void upserted(UUID owner, PlacementRecord record, boolean created) {
                log.add((created ? "new:" : "upd:") + record.id);
            }

            @Override
            public void state(UUID owner, String id, JsonObject payload) {
                log.add("state:" + payload.get("playing").getAsBoolean());
            }

            @Override
            public void removed(UUID owner, String id) {
                log.add("removed:" + id);
            }
        };
        return new SharedObjects(KIND, net.join(who), policy, listener, who, () -> clock[0], who.getLeastSignificantBits());
    }

    private PlacementRecord screen(String id) {
        WorldObject o = new WorldObject(id, KIND).dimension("minecraft:overworld");
        o.transform().size(3, 1.6875f).position(1, 64, 1);
        return PlacementCodec.encode(o, null);
    }

    private JsonObject playing(boolean on) {
        JsonObject p = new JsonObject();
        p.addProperty("playing", on);
        return p;
    }

    @Test
    void publishReachesOthersAndStateFollows() {
        aliceSide.publish(screen("s1"));
        clock[0] += 10;
        aliceSide.publishState("s1", playing(true));
        aliceSide.unpublish("s1");
        assertEquals(List.of("new:s1", "state:true", "removed:s1"), bobSaw);
    }

    @Test
    void onlyTheOwnerCanChangeAnObject() {
        aliceSide.publish(screen("s1"));
        SharedObjects mallory = node(UUID.randomUUID(), new ArrayList<>(), SyncPolicy.defaults());
        PlacementRecord fake = screen("s1");
        fake.x = 999;
        clock[0] += 10;
        mallory.publish(fake);
        assertEquals(1.0, bobSide.remote().get("s1").record().x, 1e-9);
        mallory.unpublish("s1");
        assertTrue(bobSide.remote().containsKey("s1"));
    }

    @Test
    void staleMessagesAreDropped() {
        aliceSide.publish(screen("s1"));
        long rev = bobSide.remote().get("s1").revision();
        bobSide.receive(alice, SyncMessage.state(KIND, "s1", rev, playing(true)));
        bobSide.receive(alice, SyncMessage.state(KIND, "s1", rev - 5, playing(true)));
        assertEquals(List.of("new:s1"), bobSaw);
        bobSide.receive(alice, SyncMessage.state(KIND, "s1", rev + 1, playing(false)));
        assertEquals("state:false", bobSaw.get(bobSaw.size() - 1));
    }

    @Test
    void restartedOwnerStillWinsBecauseRevisionsFollowTheClock() {
        aliceSide.publish(screen("s1"));
        clock[0] += 60_000;
        SharedObjects aliceAgain = node(alice, new ArrayList<>(), SyncPolicy.defaults());
        PlacementRecord moved = screen("s1");
        moved.x = 5;
        moved.revision = 0;
        aliceAgain.publish(moved);
        assertEquals(5.0, bobSide.remote().get("s1").record().x, 1e-9);
    }

    @Test
    void perOwnerCapAndRateLimit() {
        for (int i = 0; i < 12; i++) {
            clock[0] += 1000;
            aliceSide.publish(screen("s" + i));
        }
        assertEquals(8, bobSide.countOwnedBy(alice));
        int before = bobSaw.size();
        for (int i = 0; i < 20; i++) aliceSide.publishState("s0", playing(i % 2 == 0));
        assertTrue(bobSaw.size() - before <= 6, "burst of 6 then limited");
    }

    @Test
    void newcomersAskAndOwnersAnswerAfterARandomDelay() {
        aliceSide.publish(screen("s1"));
        UUID carol = UUID.randomUUID();
        List<String> carolSaw = new ArrayList<>();
        SharedObjects carolSide = node(carol, carolSaw, SyncPolicy.defaults());
        carolSide.requestAll();
        aliceSide.tick();
        assertTrue(carolSaw.isEmpty(), "not at once");
        long due = aliceSide.pendingAnswerAt() - clock[0];
        assertTrue(due >= 300 && due <= 1000);
        clock[0] += 1000;
        aliceSide.tick();
        assertEquals(List.of("new:s1"), carolSaw);
    }

    @Test
    void policyFilterAndLeavingPlayers() {
        SharedObjects strict = node(UUID.randomUUID(), new ArrayList<>(), SyncPolicy.defaults().accepting(r -> r.data != null));
        aliceSide.publish(screen("s1"));
        assertEquals(0, strict.remote().size());
        bobSide.forgetOwner(alice);
        assertEquals("removed:s1", bobSaw.get(bobSaw.size() - 1));
        assertEquals(0, bobSide.remote().size());
    }

    @Test
    void ownMessagesAndOtherKindsAreIgnored() {
        bobSide.receive(bob, SyncMessage.upsert(KIND, screen("x")));
        PlacementRecord other = screen("y");
        other.kind = "vibey:other";
        bobSide.receive(alice, SyncMessage.upsert("vibey:other", other));
        assertTrue(bobSaw.isEmpty());
    }

    @Test
    void codecRejectsBrokenMessages() {
        assertThrows(IllegalArgumentException.class, () -> SyncCodec.decode("{\"op\":\"UPSERT\",\"kind\":\"k\",\"id\":\"a\",\"rev\":1}"));
        assertThrows(IllegalArgumentException.class, () -> SyncCodec.decode("nonsense"));
        assertThrows(IllegalArgumentException.class, () -> SyncCodec.decode("x".repeat(SyncCodec.MAX_LENGTH + 1)));
        SyncMessage m = SyncCodec.decode(SyncCodec.encode(SyncMessage.remove(KIND, "a", 9)));
        assertEquals(SyncOp.REMOVE, m.op());
        assertEquals(9, m.revision());
    }
}

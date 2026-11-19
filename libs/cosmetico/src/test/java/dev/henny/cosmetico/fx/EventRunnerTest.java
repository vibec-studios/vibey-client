package dev.henny.cosmetico.fx;

import com.google.gson.JsonObject;
import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.fixtures.TestEngine;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.state.Avatar;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventRunnerTest {
    private final TestEngine t = new TestEngine();
    private final EventRunner runner = t.engine.eventRunner();
    private final Avatar a = new Avatar(UUID.randomUUID(), false);

    private Bundle hat(String idle, String walk) {
        JsonObject o = Fixtures.cosmetic("t:hat", "hat");
        if (idle != null) Fixtures.animation(o, "idle", idle);
        if (walk != null) Fixtures.animation(o, "walk", walk);
        Bundle b = Fixtures.parse(o);
        t.add(b);
        a.loadout.base[Slot.HAT.ordinal()] = "t:hat";
        return b;
    }

    private static String events(String type, float... times) {
        StringBuilder s = new StringBuilder("{\"length\":1,\"loop\":true,\"events\":[");
        for (int i = 0; i < times.length; i++) {
            if (i > 0) s.append(',');
            s.append("{\"t\":").append(times[i]).append(",\"type\":\"").append(type).append("\",")
                    .append(type.equals("sound") ? "\"sound\":\"minecraft:block.note_block.bell\"}" : "\"particle\":\"end_rod\",\"count\":2}");
        }
        return s.append("]}").toString();
    }

    @Test
    void idleEventsFireWhenTheirTimeIsCrossedAndWrapInLoops() {
        hat(events("particle", 0.2f, 0.8f), null);
        runner.tick(a, 10.1);
        assertEquals(0, t.fx.calls.size());
        runner.tick(a, 10.5);
        assertEquals(1, t.fx.calls.size());
        runner.tick(a, 11.3);
        assertEquals(List.of("particle minecraft:end_rod 2 0.0 1.2 0.0", "particle minecraft:end_rod 2 0.0 1.2 0.0",
                "particle minecraft:end_rod 2 0.0 1.2 0.0"), t.fx.calls);
    }

    @Test
    void otherStatesFireOnlyWhileTheyLead() {
        hat(null, events("sound", 0.5f));
        a.motion.walk = 0.4f;
        runner.tick(a, 10.0);
        runner.tick(a, 10.9);
        assertEquals(0, t.fx.calls.size());
        a.motion.walk = 0.6f;
        runner.tick(a, 11.0);
        runner.tick(a, 11.9);
        assertEquals(List.of("sound minecraft:block.note_block.bell 1.0"), t.fx.calls);
    }

    @Test
    void volumeZeroAndHiddenAvatarsStayQuiet() {
        hat(events("sound", 0.5f), null);
        t.engine.config.emoteVolume = 0f;
        runner.tick(a, 10.0);
        runner.tick(a, 10.9);
        t.engine.config.emoteVolume = 1f;
        a.frame.hidden = true;
        runner.tick(a, 11.0);
        runner.tick(a, 11.9);
        assertEquals(0, t.fx.calls.size());
    }

    @Test
    void emoteEventsIncludeTimeZeroOnTheFirstTick() {
        JsonObject o = Fixtures.emote("t:e");
        o.getAsJsonObject("animations").getAsJsonObject("main").add("events",
                Fixtures.json("{\"e\":[{\"t\":0,\"type\":\"particle\",\"particle\":\"minecraft:heart\"}]}").get("e"));
        t.add(Fixtures.parse(o));
        t.engine.emotes.play(a, "t:e");
        runner.tick(a, 0);
        runner.tick(a, 0);
        assertEquals(1, t.fx.calls.size());
    }

    @Test
    void locatorPositionsWinOverTheFallback() {
        double[] out = new double[3];
        a.frame.x = 1;
        a.frame.y = 2;
        a.frame.z = 3;
        FxMath.position(a, null, out);
        assertEquals(3.2, out[1], 1e-9);
        a.locators.set("tip", 7, 8, 9);
        FxMath.position(a, new dev.henny.cosmetico.model.Locator("tip", 0, null, new float[3]), out);
        assertEquals(8, out[1], 1e-9);
        float[] w = new float[3];
        FxMath.toWorld(0f, 1f, 0f, 0f, w);
        assertEquals(-1f, w[0], 1e-6f);
    }
}

package dev.henny.companio.world;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.pack.Pack;
import dev.henny.companio.sense.Senses;
import dev.henny.companio.support.GridView;
import dev.henny.companio.support.TestPacks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtrasTest {
    private final Senses senses = new Senses();
    private final GridView world = new GridView().floor(63);
    private double now;

    private Companion companion(String props, String behaviors) {
        String json = TestPacks.companion(behaviors).replace("\"props\":{\"hat\":{\"bones\":[\"hat\"]}}", "\"props\":" + props);
        Pack p = TestPacks.load(TestPacks.files(json));
        assertEquals(0, p.errors(), () -> p.problems().toString());
        CompanionDef def = p.companions().get(0);
        Companion c = new Companion(def, senses, 7, () -> "en_us");
        senses.owner.present = true;
        senses.owner.x = 0.5;
        senses.owner.y = 64;
        senses.owner.z = 0.5;
        senses.owner.eyeY = 65.62;
        c.spawn(0.5, 64, 2.5, 0);
        return c;
    }

    private void run(Companion c, double seconds) {
        for (int i = 0; i < Math.round(seconds * 20); i++) {
            now += 0.05;
            c.tick(world, now, 0.05f);
        }
    }

    @Test
    void propWhenFollowsItsExpressionUnlessABehaviorOverrides() {
        Companion c = companion("{\"hat\":{\"bones\":[\"hat\"],\"when\":\"weather.rain\"}}",
                "[{\"id\":\"bare\",\"priority\":5,\"when\":\"time.night\",\"props\":{\"hat\":false}}]");
        run(c, 0.2);
        assertFalse(c.propShown("hat"));
        senses.raining = true;
        run(c, 0.2);
        assertTrue(c.propShown("hat"));
        senses.dayTime = 15000;
        run(c, 0.2);
        assertFalse(c.propShown("hat"), "the running behavior hides it");
    }

    @Test
    void propWhenIsValidated() {
        String json = TestPacks.companion("[]").replace("\"props\":{\"hat\":{\"bones\":[\"hat\"]}}",
                "\"props\":{\"hat\":{\"bones\":[\"hat\"],\"when\":\"wether.rain\"}}");
        Pack p = TestPacks.load(TestPacks.files(json));
        assertTrue(p.problems().stream().anyMatch(x -> x.format().contains("props.hat.when")), p.problems().toString());
    }

    @Test
    void targetEventsAndFunction() {
        Companion c = companion("{\"hat\":{\"bones\":[\"hat\"]}}",
                "[{\"id\":\"scan\",\"priority\":50,\"on\":\"target_change\",\"when\":\"target('*_ore') && player.target != 'minecraft:stone'\",\"duration\":1,\"look\":\"target\"}]");
        run(c, 0.2);
        senses.owner.target = "minecraft:deepslate_iron_ore";
        senses.owner.targetX = 3.5;
        senses.owner.targetY = 64.5;
        senses.owner.targetZ = 6.5;
        run(c, 0.1);
        assertEquals("scan", c.brain().current().def.id());
        run(c, 1.2);
        senses.owner.target = "minecraft:stone";
        run(c, 0.1);
        assertNull(c.brain().current());
    }

    @Test
    void levelUpAdvancementAndMusicEvents() {
        Companion c = companion("{\"hat\":{\"bones\":[\"hat\"]}}", """
                [{"id":"cheer","priority":50,"on":"owner_level_up","duration":1},
                 {"id":"proud","priority":50,"on":"owner_advancement","duration":1},
                 {"id":"dance","priority":40,"on":"music_start","duration":1},
                 {"id":"quiet","priority":40,"on":"music_stop","duration":1}]""");
        run(c, 0.2);
        senses.owner.level = 3;
        run(c, 0.1);
        assertEquals("cheer", c.brain().current().def.id());
        run(c, 1.2);
        senses.owner.advancement = true;
        run(c, 0.1);
        assertEquals("proud", c.brain().current().def.id());
        run(c, 1.2);
        senses.music = true;
        run(c, 0.1);
        assertEquals("dance", c.brain().current().def.id());
        run(c, 1.2);
        senses.music = false;
        run(c, 0.1);
        assertEquals("quiet", c.brain().current().def.id());
    }

    @Test
    void climbIsNegativeWhileFalling() {
        Companion c = companion("{\"hat\":{\"bones\":[\"hat\"]}}",
                "[{\"id\":\"down\",\"priority\":50,\"when\":\"self.climb < -2\",\"move\":\"stay\",\"minDuration\":0}]");
        run(c, 0.5);
        c.place(0.5, 70, 2.5);
        boolean seen = false;
        for (int i = 0; i < 30 && !seen; i++) {
            run(c, 0.05);
            seen = c.brain().current() != null && "down".equals(c.brain().current().def.id());
        }
        assertTrue(seen, "falling makes self.climb negative");
    }
}

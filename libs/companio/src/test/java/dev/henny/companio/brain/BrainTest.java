package dev.henny.companio.brain;

import dev.henny.companio.def.AnimRef;
import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.def.ParticleDef;
import dev.henny.companio.def.SoundDef;
import dev.henny.companio.sense.Senses;
import dev.henny.companio.support.TestPacks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BrainTest {
    private final List<String> log = new ArrayList<>();
    private final Senses senses = new Senses();
    private double now;

    private final Effects fx = new Effects() {
        public void anim(AnimRef anim, boolean untilEnd) { log.add("anim " + anim.name()); }
        public void stopAnim(String name) { log.add("stop " + name); }
        public void prop(String prop, boolean show) { log.add("prop " + prop + "=" + show); }
        public void sound(SoundDef sound) { log.add("sound " + sound.id()); }
        public void particle(ParticleDef particle) { log.add("particle " + particle.id()); }
        public void say(String text, float seconds) { log.add("say " + text); }
        public void hop(float height) { log.add("hop"); }
    };

    private Brain brain(String behaviors) {
        CompanionDef def = TestPacks.blob(behaviors);
        return new Brain(def, senses, new Self(), new Random(1), () -> fx, () -> "en_us");
    }

    private void run(Brain b, double seconds) {
        for (int i = 0; i < Math.round(seconds * 20); i++) {
            now += 0.05;
            b.tick(now, 0.05f);
        }
    }

    private static String id(Brain b) {
        return b.current() == null ? null : b.current().def.id();
    }

    @Test
    void highestPriorityWinsAndConditionsEnd() {
        Brain b = brain("""
                [{"id":"low","priority":1},
                 {"id":"rain","priority":50,"when":"weather.rain","minDuration":0.5}]""");
        run(b, 0.1);
        assertEquals("low", id(b));
        senses.raining = true;
        run(b, 0.1);
        assertEquals("rain", id(b));
        senses.raining = false;
        run(b, 1.0);
        assertEquals("low", id(b));
    }

    @Test
    void eventsTriggerAndCooldownsBlock() {
        Brain b = brain("""
                [{"id":"base","priority":1},
                 {"id":"petted","priority":90,"on":"pet","cooldown":5,"anim":"clap",
                  "start":[{"do":"say","text":"@say.hi"},{"do":"sound","name":"boop","at":0.3},{"do":"var","name":"count","value":"var.count + 1"}]}]""");
        run(b, 0.1);
        b.fire("pet");
        run(b, 0.1);
        assertEquals("petted", id(b));
        assertTrue(log.contains("anim clap"));
        assertTrue(log.stream().anyMatch(s -> s.equals("say Hi") || s.equals("say Hello")), log.toString());
        assertFalse(log.contains("sound boop"), "delayed by at");
        run(b, 0.4);
        assertTrue(log.contains("sound boop"));
        assertEquals(1.0, b.memory().var("count"));
        run(b, 1.0);
        assertEquals("base", id(b), "a triggered behavior lasts as long as its animation");
        assertTrue(log.contains("stop clap"));
        b.fire("pet");
        run(b, 0.1);
        assertEquals("base", id(b), "cooldown");
        assertTrue(b.memory().since("pet", now) < 0.5);
    }

    @Test
    void repeatsNeedsAndInterrupt() {
        Brain b = brain("""
                [{"id":"sleep","priority":10,"interrupt":false,"needs":{"energy":3},
                  "repeat":[{"every":1,"actions":[{"do":"particle","name":"hearts"}]}]},
                 {"id":"other","priority":20,"when":"since('pet') < 100"}]""");
        b.needs().set("energy", 0.1f);
        run(b, 0.05);
        b.fire("pet");
        run(b, 3.1);
        assertEquals("sleep", id(b), "interrupt false keeps it");
        assertEquals(3, log.stream().filter(s -> s.equals("particle hearts")).count());
        assertTrue(b.needs().get("energy") > 0.15f, "needs rise while sleeping: " + b.needs().get("energy"));
    }

    @Test
    void tricksForceTheirBehavior() {
        Brain b = brain("""
                [{"id":"base","priority":100},
                 {"id":"spin","priority":1,"duration":1,"anim":{"name":"spin","blend":"replace"},"props":{"hat":true}}]""");
        run(b, 0.1);
        assertEquals("base", id(b));
        assertTrue(b.trick("spin"));
        assertEquals("spin", id(b));
        run(b, 0.5);
        assertEquals("spin", id(b), "forced behaviors are not interrupted");
        run(b, 0.6);
        assertEquals("base", id(b));
    }

    @Test
    void randomPicksOneList() {
        Brain b = brain("""
                [{"id":"r","priority":1,"start":[{"do":"random","of":[[{"do":"hop"}],[{"do":"sound","name":"boop"}]]}]}]""");
        run(b, 0.1);
        assertEquals(1, log.stream().filter(s -> s.equals("hop") || s.equals("sound boop")).count());
    }

    @Test
    void chanceIsPerSecond() {
        Brain b = brain("""
                [{"id":"x","priority":5,"when":"chance(0.5)","duration":0.05,"cooldown":0},{"id":"base","priority":1}]""");
        int starts = 0;
        for (int i = 0; i < 2000; i++) {
            run(b, 0.05);
            if ("x".equals(id(b)) && b.current().started == now) starts++;
        }
        assertTrue(starts > 25 && starts < 75, "about 50 starts in 100 s, got " + starts);
    }
}

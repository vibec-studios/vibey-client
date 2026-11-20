package dev.henny.companio.parse;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.pack.Pack;
import dev.henny.companio.pack.Problem;
import dev.henny.companio.support.TestPacks;
import dev.henny.companio.text.TextRef;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PackReaderTest {
    private static final String OK = """
            [{"id":"idle_spin","priority":5,"when":"need.energy > 0.5 && chance(0.1)","duration":[1,2],"anim":"spin",
              "start":[{"do":"say","text":"@say.hi"},{"do":"sound","name":"boop","at":0.5},{"do":"particle","name":"hearts"}]},
             {"id":"spin","priority":10,"anim":{"name":"spin","blend":"replace"},"props":{"hat":true}}]""";

    private static String errors(Pack p) {
        return p.problems().stream().map(Problem::toString).collect(Collectors.joining("\n"));
    }

    @Test
    void readsAValidPack() {
        Pack p = TestPacks.load(TestPacks.files(TestPacks.companion(OK)));
        assertEquals(0, p.errors(), errors(p));
        assertEquals("test", p.id());
        assertEquals("Test DE", p.name().resolve(p.lang(), "de_de"));
        CompanionDef c = p.companions().get(0);
        assertEquals("test:blob", c.key());
        assertEquals("Klecks", c.name().resolve(c.lang(), "de_de"));
        assertEquals("Blob", c.name().resolve(c.lang(), "fr_fr"));
        assertEquals(2, c.behaviors().size());
        assertEquals("walk", c.loco().get(dev.henny.companio.def.LocoState.RUN));
        assertEquals("idle", c.loco().get(dev.henny.companio.def.LocoState.SLEEP));
        assertEquals(1, c.props().get("hat").boneIndices().length);
        assertEquals(16, p.hash().length());
    }

    @Test
    void reportsFileAndPathForMistakes() {
        String bad = """
                [{"id":"a","priority":5,"when":"wether.rain","prioirty":3},
                 {"id":"b","on":"pett","start":[{"do":"jump"},{"do":"prop","name":"umbrella"},{"do":"sound","name":"nope"}]},
                 {"id":"c","anim":"dance","move":"teleport"}]""";
        Pack p = TestPacks.load(TestPacks.files(TestPacks.companion(bad)));
        String all = errors(p);
        assertTrue(p.errors() >= 6, all);
        assertTrue(all.contains("companions/blob.json: behaviors[0].when → unknown variable \"wether.rain\", did you mean \"weather.rain\"?"), all);
        assertTrue(all.contains("behaviors[0].prioirty → unknown field \"prioirty\", did you mean \"priority\"?"), all);
        assertTrue(all.contains("behaviors[1].on → unknown event \"pett\", did you mean \"pet\"?"), all);
        assertTrue(all.contains("unknown action \"jump\""), all);
        assertTrue(all.contains("no prop \"umbrella\""), all);
        assertTrue(all.contains("the model has no animation \"dance\""), all);
        assertTrue(p.companions().isEmpty(), "a companion with errors is not loaded");
    }

    @Test
    void reportsBrokenFiles() {
        Map<String, String> f = TestPacks.files(TestPacks.companion(OK));
        f.put("companion.json", "{\"format\":\"companio-pack\",\"formatVersion\":7,\"id\":\"x\"}");
        assertTrue(errors(TestPacks.load(f)).contains("needs a newer companio"));
        f = TestPacks.files(TestPacks.companion(OK));
        f.put("companions/blob.json", "{ not json");
        Pack p = TestPacks.load(f);
        assertTrue(errors(p).contains("companions/blob.json → invalid JSON"), errors(p));
        f = TestPacks.files(TestPacks.companion(OK).replace("\"locator\":\"top\"", "\"locator\":\"tail\""));
        Pack tail = TestPacks.load(f);
        assertTrue(errors(tail).contains("particles.hearts.locator → the model has no locator \"tail\""), errors(tail));
        f = TestPacks.files(TestPacks.companion(OK).replace("\"bones\":[\"hat\"]", "\"bones\":[\"hta\"]"));
        Pack hta = TestPacks.load(f);
        assertTrue(errors(hta).contains("props.hat.bones[0] → the model has no bone \"hta\", did you mean \"hat\"?"), errors(hta));
        f = TestPacks.files(TestPacks.companion(OK));
        f.remove("models/blob.vibey");
        assertTrue(errors(TestPacks.load(f)).contains("models/blob.vibey → file is missing"));
    }

    @Test
    void variantsReplaceTextures() {
        String c = TestPacks.companion(OK).replace("\"scale\":1,", "\"scale\":1,\"variants\":[{\"id\":\"a\"},{\"id\":\"b\",\"textures\":{\"main\":\"textures/b.png\"}}],");
        Map<String, String> f = TestPacks.files(c);
        Pack missing = TestPacks.load(f);
        assertTrue(errors(missing).contains("variants.b.textures.main → file \"textures/b.png\" is missing"), errors(missing));
        var files = dev.henny.companio.pack.MapFiles.ofText("test", f).with("textures/b.png", java.util.Base64.getDecoder().decode(TestPacks.PNG_1x1));
        Pack p = PackReader.read(files, dev.henny.cosmetico.version.VersionGate.fixed("99.0.0"), false);
        assertEquals(0, p.errors(), errors(p));
        CompanionDef d = p.companions().get(0);
        assertNotSame(d.model("a"), d.model("b"));
        assertSame(d.model("a"), d.model("nope"));
    }

    @Test
    void textsResolveWithFallbacks() {
        TextRef t = TextRef.languages(Map.of("en", "Hi", "de", "Hallo"));
        assertEquals("Hallo", t.resolve(null, "de_at"));
        assertEquals("Hi", t.resolve(null, "fr_fr"));
        assertEquals("@missing", TextRef.key("missing").resolve(dev.henny.companio.text.Lang.EMPTY, "en_us"));
    }
}

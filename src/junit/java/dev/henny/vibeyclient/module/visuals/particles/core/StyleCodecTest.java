package dev.henny.vibeyclient.module.visuals.particles.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyleCodecTest {
    @Test
    void roundTrips() {
        ParticleStyles styles = new ParticleStyles();
        styles.set("minecraft:crit", new ParticleStyle(250, 140, 0xFF3B3B, 70, false));
        styles.set("minecraft:rain", ParticleStyle.VANILLA.withHidden(true));
        JsonObject json = StyleCodec.write(styles);
        Map<String, ParticleStyle> back = StyleCodec.read(JsonParser.parseString(json.toString()));
        assertEquals(styles.all(), back);
    }

    @Test
    void writesOnlyWhatDiffers() {
        JsonObject o = StyleCodec.write(ParticleStyle.VANILLA.withAmount(50));
        assertEquals(1, o.size());
        assertEquals(50, o.get("amount").getAsInt());
    }

    @Test
    void skipsBrokenEntries() {
        String json = "{\"a:x\":{\"amount\":\"lots\"},\"b:y\":5,\"c:z\":{\"size\":900,\"tint\":\"#zzzzzz\",\"strength\":20},"
                + "\"d:w\":{}}";
        Map<String, ParticleStyle> m = StyleCodec.read(JsonParser.parseString(json));
        assertFalse(m.containsKey("a:x"));
        assertFalse(m.containsKey("b:y"));
        assertFalse(m.containsKey("d:w"));
        assertTrue(m.containsKey("c:z"));
        assertEquals(ParticleStyle.MAX_SIZE, m.get("c:z").size());
        assertEquals(ParticleStyle.WHITE, m.get("c:z").tint());
        assertTrue(StyleCodec.read(null).isEmpty());
    }
}

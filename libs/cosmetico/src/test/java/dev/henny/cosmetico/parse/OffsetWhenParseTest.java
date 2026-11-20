package dev.henny.cosmetico.parse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.version.VersionGate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffsetWhenParseTest {
    private static Bundle parse(String offsetWhen) {
        String json = "{\"format\":\"vibey\",\"formatVersion\":1,\"kind\":\"cosmetic\",\"id\":\"test:wings\",\"name\":{\"en\":\"W\"},"
                + "\"author\":\"t\",\"tags\":[],\"rarity\":\"common\",\"cosmetic\":{\"slot\":\"wings\""
                + (offsetWhen == null ? "" : ",\"offsetWhen\":" + offsetWhen) + "},\"textures\":[],"
                + "\"model\":{\"bones\":[{\"name\":\"w\",\"parent\":null,\"attach\":\"body\",\"pivot\":[0,21,2],\"rotation\":[0,0,0],\"cubes\":[]}]}}";
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        return Bundles.parse(o, VersionGate.fixed("0.3.1"));
    }

    @Test
    void absentMeansNoRules() {
        assertTrue(parse(null).offsetWhen.isEmpty());
    }

    @Test
    void validRules() {
        Bundle b = parse("[{\"slot\":\"back\",\"offset\":[0,1,8]},{\"slot\":\"hat\",\"rotation\":[-5,0,0]}]");
        assertEquals(2, b.offsetWhen.size());
        assertEquals(Slot.BACK, b.offsetWhen.get(0).slot());
        assertArrayEquals(new float[]{0, 1, 8}, b.offsetWhen.get(0).offset());
        assertArrayEquals(new float[]{0, 0, 0}, b.offsetWhen.get(0).rotation());
        assertArrayEquals(new float[]{-5, 0, 0}, b.offsetWhen.get(1).rotation());
        assertTrue(b.warnings.isEmpty(), b.warnings.toString());
    }

    @Test
    void malformedRulesAreSkippedNotFatal() {
        Bundle b = parse("[{\"slot\":\"tail\",\"offset\":[0,0,1]},{\"slot\":\"wings\",\"offset\":[0,0,1]},"
                + "{\"slot\":\"back\",\"offset\":[0,0,99]},{\"slot\":\"back\",\"offset\":[0,\"x\",1]},{\"slot\":\"cape\",\"offset\":[0,0,2]}]");
        assertEquals(1, b.offsetWhen.size());
        assertEquals(Slot.CAPE, b.offsetWhen.get(0).slot());
        assertEquals(4, b.warnings.size(), b.warnings.toString());
        assertTrue(parse("\"back\"").offsetWhen.isEmpty());
    }

    @Test
    void atMostEightRules() {
        String rule = "{\"slot\":\"back\",\"offset\":[0,0,1]}";
        Bundle b = parse("[" + String.join(",", java.util.Collections.nCopies(9, rule)) + "]");
        assertEquals(8, b.offsetWhen.size());
        assertEquals(1, b.warnings.size());
    }
}

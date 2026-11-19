package dev.henny.cosmetico.parse;

import com.google.gson.JsonObject;
import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.model.Attach;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Interp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForwardCompatTest {
    @Test
    void unknownFieldsAndValuesOnlyWarn() {
        JsonObject o = Fixtures.cosmetic("t:x", "hat");
        o.addProperty("futureField", 1);
        o.getAsJsonObject("model").getAsJsonArray("bones").get(0).getAsJsonObject().addProperty("attach", "tail");
        Fixtures.animation(o, "idle", "{\"length\":1,\"bones\":{\"b\":{\"rotation\":[{\"t\":0,\"v\":[0,0,0],\"i\":\"smooth\"}]}},"
                + "\"events\":[{\"t\":0,\"type\":\"confetti\"}]}");
        Bundle b = Fixtures.parse(o);
        assertEquals(Attach.BODY, b.model.bones[0].attach);
        assertEquals(Interp.LINEAR, b.animation("idle").tracks[0].rotation().interp[0]);
        assertEquals(3, b.warnings.size(), b.warnings.toString());
    }

    @Test
    void newerClientsAreRequired() {
        JsonObject o = Fixtures.cosmetic("t:x", "hat");
        o.addProperty("minClientVersion", "9.9.9");
        NeedsUpdateException e = assertThrows(NeedsUpdateException.class, () -> Fixtures.parse(o));
        assertEquals("9.9.9", e.required);
        JsonObject f = Fixtures.cosmetic("t:x", "hat");
        f.addProperty("formatVersion", 2);
        assertNull(assertThrows(NeedsUpdateException.class, () -> Fixtures.parse(f)).required);
    }

    @Test
    void textFollowsTheLanguage() {
        Bundle b = Fixtures.parse(Fixtures.cosmetic("t:x", "hat"));
        assertEquals("D", b.name.get("de"));
        assertEquals("N", b.name.get("en"));
        assertEquals("N", b.name.get("fr"));
        assertTrue(b.description.get("de").isEmpty());
    }
}

package dev.henny.vibeyclient.ranks;

import com.google.gson.JsonParser;
import dev.henny.vibeyclient.ranks.style.WaveStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RanksDocJsonTest {
    private static final String DOC = """
            { "version": "3f9c", "ranks": [
              { "id": "mod", "label": { "en": "Moderator", "de": "Moderator" }, "description": {}, "icon": "hammer",
                "color": "#3fe0c5", "priority": 80, "kind": "primary", "staff": true },
              { "id": "beta", "label": { "en": "Beta tester", "de": "Beta-Tester" }, "icon": "flask",
                "color": "#8f9aa6", "priority": 10, "kind": "badge", "staff": false },
              { "id": "BAD ID", "label": { "en": "x" }, "icon": "x", "color": "#ffffff" } ],
              "nameStyles": [
              { "id": "rainbow", "label": { "en": "Rainbow" }, "ranks": ["mod"], "sort": 2, "effect": "rainbow", "params": {} },
              { "id": "ocean-wave", "label": { "en": "Ocean wave", "de": "Meereswelle" }, "ranks": ["mod"], "sort": 1,
                "effect": "wave", "params": { "colors": ["#3fe0c5", "#4aa8ff"], "speed": 0.8, "wavelength": 6, "direction": "ltr" } },
              { "id": "broken", "label": { "en": "?" }, "ranks": [], "sort": 0, "effect": "sparkle", "params": {} } ] }
            """;

    @Test
    void parsesRanksAndStyles() {
        RanksDoc d = RanksDocJson.parse(JsonParser.parseString(DOC).getAsJsonObject());
        assertEquals("3f9c", d.version());
        assertEquals(2, d.ranks().size());
        RankDef mod = d.rank("mod");
        assertEquals(0x3fe0c5, mod.color());
        assertTrue(mod.staff() && mod.canBePrimary());
        assertEquals("Beta-Tester", d.rank("beta").label().get(true));
        assertEquals(List.of("ocean-wave", "rainbow"), d.styles().stream().map(NameStyleDef::id).toList());
        assertInstanceOf(WaveStyle.class, d.style("ocean-wave").style());
        assertEquals("Rainbow", d.style("rainbow").label().get(true));
        assertNull(d.style("broken"));
    }

    @Test
    void notADoc() {
        assertNull(RanksDocJson.parse(JsonParser.parseString("{\"error\":{}}").getAsJsonObject()));
    }
}

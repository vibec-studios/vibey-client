package dev.henny.vibeyclient.ranks.style;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NameStyleVectorsTest {
    @Test
    void reproducesEverySharedVector() throws Exception {
        JsonObject root = load();
        Map<String, NameStyle> styles = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("styles").entrySet()) {
            NameStyle s = NameStyleJson.parse(e.getValue().getAsJsonObject());
            assertNotNull(s, e.getKey());
            styles.put(e.getKey(), s);
        }
        JsonArray cases = root.getAsJsonArray("cases");
        assertEquals(280, cases.size());
        for (JsonElement ce : cases) check(styles, ce.getAsJsonObject());
    }

    private static void check(Map<String, NameStyle> styles, JsonObject c) {
        NameStyle s = styles.get(c.get("style").getAsString());
        int length = c.get("length").getAsInt();
        double t = c.get("t").getAsDouble();
        JsonArray want = c.getAsJsonArray("colors");
        for (int i = 0; i < length; i++) {
            String got = ColorMath.hex(s.colorAt(i, length, t));
            assertEquals(want.get(i).getAsString(), got, c.get("style").getAsString() + " len=" + length + " t=" + t + " i=" + i);
        }
    }

    private static JsonObject load() throws Exception {
        try (InputStream in = NameStyleVectorsTest.class.getResourceAsStream("/ranks/name-style-vectors.json")) {
            assertNotNull(in, "vectors resource");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}

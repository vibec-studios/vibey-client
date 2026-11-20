package dev.henny.vibeyclient.ui.vui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabelRulesTest {
    private static final int MAX_WORDS = 3;
    private static final int MAX_ACTION = 22;

    @Test
    void settingLabelsStayShort() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String lang : new String[] {"en_us", "de_de"}) {
            Path dir = root().resolve("src/main/resources/assets/vibeyclient/i18n").resolve(lang);
            try (Stream<Path> files = Files.list(dir)) {
                for (Path f : files.toList()) {
                    JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                    walk(lang + "/" + f.getFileName(), "", o, bad);
                }
            }
        }
        assertEquals("[]", bad.toString());
    }

    private static void walk(String file, String prefix, JsonObject o, List<String> bad) {
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            if (e.getValue().isJsonObject()) walk(file, key, e.getValue().getAsJsonObject(), bad);
            else if (e.getValue().isJsonPrimitive() && label(key)) check(file, key, e.getValue().getAsString(), bad);
        }
    }

    private static boolean label(String key) {
        if (!key.startsWith("setting.") && !key.startsWith("section.")) return false;
        return !key.endsWith(".desc") && !key.endsWith("Tip") && !key.endsWith(".tip") && !key.endsWith(".button");
    }

    private static void check(String file, String key, String value, List<String> bad) {
        int words = value.trim().isEmpty() ? 0 : value.trim().split("\s+").length;
        if (words > MAX_WORDS) bad.add(file + " " + key + "=" + value);
        else if (key.endsWith(".action") && value.length() > MAX_ACTION) bad.add(file + " " + key + "=" + value);
    }

    private static Path root() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("stonecutter.gradle"))) p = p.getParent();
        return p;
    }
}

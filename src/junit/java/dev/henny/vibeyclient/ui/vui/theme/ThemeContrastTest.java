package dev.henny.vibeyclient.ui.vui.theme;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.api.GlobalSettings.UiTheme;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeContrastTest {
    private static final int[] ACCENTS = {0xFF7759FF, 0xFF2F8CFF, 0xFF13A387, 0xFFE0457B, 0xFFE07A1F, 0xFF9A9AA6, 0xFFFFE066, 0xFF202020};

    static Map<String, String> tokens(UiTheme theme) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String file : UiThemes.files(theme)) {
            InputStream in = ThemeContrastTest.class.getResourceAsStream("/" + file);
            assertNotNull(in, file);
            JsonObject colors = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("colors");
            for (String k : colors.keySet()) out.put(k, colors.get(k).getAsString());
        }
        return out;
    }

    @Test
    void everyThemePassesWcagAa() {
        for (UiTheme t : UiTheme.values()) {
            Map<String, String> tokens = tokens(t);
            System.out.println("[contrast] " + t + " " + ThemeAudit.contrast(tokens));
            assertEquals("[]", ThemeAudit.failures(tokens).toString(), t.name());
        }
    }

    @Test
    void colourBlindSetsSeparateStatusColours() {
        Map<String, String> dark = tokens(UiTheme.DARK);
        check(UiTheme.DEUTAN, ColorVision.DEUTAN, dark);
        check(UiTheme.PROTAN, ColorVision.PROTAN, dark);
        check(UiTheme.TRITAN, ColorVision.TRITAN, dark);
    }

    @Test
    void customAccentKeepsButtonTextReadable() {
        for (UiTheme t : new UiTheme[] {UiTheme.DARK, UiTheme.LIGHT}) {
            for (int a : ACCENTS) {
                Map<String, String> tokens = tokens(t);
                tokens.putAll(Accent.tokens(a, tokens.get("surface3")));
                assertEquals("[]", ThemeAudit.failures(tokens).toString(), t + " " + Contrast.hex(a));
            }
        }
    }

    @Test
    void parsesAlphaHex() {
        assertEquals(0x17FFFFFF, Contrast.parse("#ffffff17"));
        assertEquals(0xFF112233, Contrast.parse("#123"));
        assertEquals(21.0, Contrast.ratio(0xFFFFFFFF, 0xFF000000), 0.01);
    }

    private static void check(UiTheme theme, ColorVision vision, Map<String, String> dark) {
        double own = ThemeAudit.statusDistance(tokens(theme), vision);
        double base = ThemeAudit.statusDistance(dark, vision);
        System.out.printf("[contrast] %s under %s: status dE %.0f (dark %.0f)%n", theme, vision, own, base);
        assertTrue(own >= 2 * ThemeAudit.STATUS_DISTANCE, theme + " dE " + own);
        assertTrue(own > base, theme + " should separate better than the dark theme");
    }
}

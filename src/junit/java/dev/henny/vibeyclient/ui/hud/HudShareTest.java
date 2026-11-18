package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Share-code codec and style JSON: round trips, and bad input only ever gives {@link HudShare.BadLayout}. */
class HudShareTest {
    private static final String LAYOUT = "{\"v\":1,\"g\":{\"bg\":true,\"bo\":0.55,\"co\":\"bevel\",\"ac\":\"8b6cff\"},"
            + "\"e\":{\"hud.fps\":{\"a\":\"top_left\",\"x\":4,\"y\":4,\"s\":1.0},"
            + "\"hud.items.2\":{\"a\":\"bottom\",\"x\":160,\"y\":-3,\"s\":1.0,\"o\":{\"item\":\"minecraft:totem_of_undying\"}}},"
            + "\"c\":{\"hud.items\":[2]}}";

    @Test
    void roundTrip() throws Exception {
        JsonObject json = JsonParser.parseString(LAYOUT).getAsJsonObject();
        String code = HudShare.encode(json);
        assertTrue(code.startsWith(HudShare.PREFIX));
        assertTrue(code.length() < LAYOUT.length(), "compressed: " + code.length());
        assertEquals(json, HudShare.decode(code));
        // whitespace, quotes and line breaks from chat or a text file are fine
        assertEquals(json, HudShare.decode("  \"" + code.substring(0, 20) + "\n" + code.substring(20) + "\"  "));
        // a pasted layout JSON too
        assertEquals(json, HudShare.decode(LAYOUT));
    }

    @Test
    void rejectsGarbage() {
        String[] bad = {null, "", "   ", "hello", "VHUD1.", "VHUD1.%%%", "VHUDx.abc", "{", "[1,2]", "{\"v\":1}",
                "{\"e\":[]}", "VHUD1." + "A".repeat(HudShare.MAX_CODE + 10), "{".repeat(100_000),
                "VHUD1.eJzLSM3JyQcABiwCFQ"};
        for (String b : bad) {
            try {
                HudShare.decode(b);
                fail("accepted: " + (b == null ? "null" : b.substring(0, Math.min(30, b.length()))));
            } catch (HudShare.BadLayout e) {
                assertTrue(e.key.startsWith("core.hud.share.error."), e.key);
            } catch (RuntimeException e) {
                fail("crashed on " + (b == null ? "null" : b.substring(0, Math.min(30, b.length()))), e);
            }
        }
        HudShare.BadLayout newer = assertThrows(HudShare.BadLayout.class, () -> HudShare.decode("VHUD2.abcd"));
        assertEquals("core.hud.share.error.newer", newer.key);
        newer = assertThrows(HudShare.BadLayout.class, () -> HudShare.decode("{\"v\":7,\"e\":{}}"));
        assertEquals("core.hud.share.error.newer", newer.key);
    }

    @Test
    void randomBytesNeverCrash() {
        Random r = new Random(42);
        for (int i = 0; i < 2000; i++) {
            byte[] b = new byte[r.nextInt(200)];
            r.nextBytes(b);
            String s = HudShare.PREFIX + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
            try {
                HudShare.decode(s);
            } catch (HudShare.BadLayout expected) {
                // fine
            } catch (RuntimeException e) {
                fail("crashed on random input " + i, e);
            }
        }
    }

    @Test
    void styleJson() {
        HudStyle s = new HudStyle();
        s.background = false;
        s.textColor = 0xFF5FA8;
        s.paint = HudStyle.TextPaint.RAINBOW;
        s.bgOpacity = 0.4f;
        HudStyle back = HudStyle.fromJson(s.toJson());
        assertEquals(s, back);
        // bad values are dropped, not thrown
        HudStyle junk = HudStyle.fromJson(JsonParser.parseString("{\"bg\":\"yes\",\"tc\":\"#zzzzzz\",\"bo\":5,\"tp\":\"neon\",\"co\":7}"));
        assertNull(junk.background);
        assertNull(junk.textColor);
        assertEquals(1f, junk.bgOpacity);
        assertNull(junk.paint);
        assertNull(junk.corners);
    }

    @Test
    void resolveInherits() {
        HudStyle global = HudStyle.defaults();
        global.accentColor = 0x3FE0C5;
        HudStyle own = new HudStyle();
        own.textColor = 0x112233;
        HudStyle.Resolved r = HudStyle.resolve(global, own, new HudStyle.Resolved());
        assertEquals(0xFF3FE0C5, r.accent);
        assertEquals(0xFF112233, r.text);
        assertEquals(Math.round(0.62f * 255), r.bg >>> 24);
    }
}

package dev.henny.vibeyclient.module.screens.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodecTest {
    @Test
    void roundTrip() {
        Placement p = new Placement(new Pos(2, 0, -4.5f, 12f), 1.5f, 220, true, "button@3");
        ScreenLayout l = new ScreenLayout(1.25f, 40, 3, true, Map.of("button:gui.done", p, "slider:x", Placement.DEFAULT), "Options");
        Map<String, ScreenLayout> in = new LinkedHashMap<>();
        in.put("net.minecraft.client.gui.screens.options.OptionsScreen", l);
        in.put("empty", ScreenLayout.EMPTY);
        Map<String, ScreenLayout> out = LayoutCodec.read(LayoutCodec.write(in).toString());
        assertEquals(1, out.size());
        ScreenLayout back = out.get("net.minecraft.client.gui.screens.options.OptionsScreen");
        assertEquals(l, back);
        assertEquals(p, back.placement("button:gui.done"));
        assertEquals(1, back.widgets().size());
    }

    @Test
    void badInputGivesNothingOrDefaults() {
        assertTrue(LayoutCodec.read("{nope").isEmpty());
        assertTrue(LayoutCodec.read("[]").isEmpty());
        assertTrue(LayoutCodec.read(null).isEmpty());
        Map<String, ScreenLayout> m = LayoutCodec.read("{\"screens\":{\"a\":{\"scale\":\"big\",\"dim\":500,"
                + "\"widgets\":{\"w\":{\"ax\":9,\"ay\":-3,\"x\":1,\"y\":2,\"scale\":0}}}}}");
        ScreenLayout a = m.get("a");
        assertEquals(1f, a.scale());
        assertEquals(ScreenLayout.MAX_DIM, a.dim());
        Placement w = a.placement("w");
        assertEquals(2, w.pos().ax());
        assertEquals(0, w.pos().ay());
        assertEquals(Placement.MIN_SCALE, w.scale());
    }

    @Test
    void storeDropsEmptyLayoutsAndNotifies() {
        LayoutStore s = new LayoutStore();
        int[] n = {0};
        s.onChange(() -> n[0]++);
        s.put("a", ScreenLayout.EMPTY.withHideTitle(true));
        s.put("b", ScreenLayout.EMPTY);
        assertEquals(java.util.List.of("a"), s.names());
        s.remove("a");
        assertTrue(s.names().isEmpty());
        assertEquals(3, n[0]);
    }
}

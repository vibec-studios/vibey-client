package dev.henny.vibeyclient.module.screens.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentityTest {
    private static WidgetIds.Fact f(String type, String key) {
        return new WidgetIds.Fact(type, key);
    }

    @Test
    void keysTypesAndOccurrences() {
        List<WidgetIds.Id> ids = WidgetIds.assign(List.of(f("button", "gui.done"), f("button", null), f("slider", "options.fov"),
                f("button", "gui.done"), f(null, "x")));
        assertEquals("button:gui.done", ids.get(0).primary());
        assertEquals("button@0", ids.get(0).fallback());
        assertEquals("button@1", ids.get(1).primary());
        assertEquals("slider:options.fov", ids.get(2).primary());
        assertEquals("button:gui.done#1", ids.get(3).primary());
        assertEquals("button@2", ids.get(3).fallback());
        assertEquals("widget:x", ids.get(4).primary());
    }

    @Test
    void matchesByKeyFirst() {
        List<WidgetIds.Id> ids = WidgetIds.assign(List.of(f("button", "a"), f("button", "b")));
        Map<String, Placement> saved = Map.of("button:b", Placement.DEFAULT.withHidden(true).withFallback("button@0"));
        assertArrayEquals(new String[] {null, "button:b"}, IdMatcher.match(ids, saved));
    }

    @Test
    void fallsBackToTypeIndexWhenTextChanged() {
        List<WidgetIds.Id> ids = WidgetIds.assign(List.of(f("button", "Server: 3 players"), f("button", "b")));
        Map<String, Placement> saved = Map.of("button:Server: 2 players", Placement.DEFAULT.withScale(2f).withFallback("button@0"));
        assertArrayEquals(new String[] {"button:Server: 2 players", null}, IdMatcher.match(ids, saved));
    }

    @Test
    void fallbackNeverStealsAWidgetWithItsOwnEntry() {
        List<WidgetIds.Id> ids = WidgetIds.assign(List.of(f("button", "b"), f("button", "c")));
        Map<String, Placement> saved = new java.util.LinkedHashMap<>();
        saved.put("button:b", Placement.DEFAULT.withScale(2f).withFallback("button@0"));
        saved.put("button:gone", Placement.DEFAULT.withScale(3f).withFallback("button@0"));
        assertArrayEquals(new String[] {"button:b", null}, IdMatcher.match(ids, saved));
    }
}

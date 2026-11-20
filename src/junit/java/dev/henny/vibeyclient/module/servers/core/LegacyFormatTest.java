package dev.henny.vibeyclient.module.servers.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyFormatTest {
    @Test
    void plainTextStaysOneSegment() {
        MotdSegment s = new MotdSegment("Hello", "#a1a1aa", false, false);
        assertEquals(List.of(s), LegacyFormat.split(s));
    }

    @Test
    void colourAndBoldCodesSplitRuns() {
        List<MotdSegment> out = LegacyFormat.split(new MotdSegment("§aGreen §lBold§r plain", "#a1a1aa", false, false));
        assertEquals(3, out.size());
        assertEquals(new MotdSegment("Green ", "#55ff55", false, false), out.get(0));
        assertEquals(new MotdSegment("Bold", "#55ff55", true, false), out.get(1));
        assertEquals(new MotdSegment(" plain", "#a1a1aa", false, false), out.get(2));
    }

    @Test
    void trailingSectionSignIsKept() {
        List<MotdSegment> out = LegacyFormat.split(new MotdSegment("50§", "#ffffff", false, false));
        assertTrue(out.get(0).text().endsWith("§"));
    }
}

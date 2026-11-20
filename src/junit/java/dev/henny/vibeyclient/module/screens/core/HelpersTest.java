package dev.henny.vibeyclient.module.screens.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpersTest {
    @Test
    void scaleSteps() {
        assertEquals(1.1f, ScaleSteps.next(1f, 1, 0.5f, 3f));
        assertEquals(0.9f, ScaleSteps.next(1f, -1, 0.5f, 3f));
        assertEquals(2f, ScaleSteps.next(2f, 1, 0.5f, 2f));
        assertEquals(1.25f, ScaleSteps.next(1.2f, 1, 0.5f, 3f));
        assertEquals(125, ScaleSteps.percent(1.25f));
    }

    @Test
    void historyUndoRedoAndMerge() {
        History<String> h = new History<>();
        h.record("a", "move", 0);
        h.record("b", "wheel", 10);
        h.record("c", "wheel", 300);
        assertEquals("b", h.undo("d"));
        assertEquals("a", h.undo("b"));
        assertNull(h.undo("a"));
        assertEquals("b", h.redo("a"));
        h.record("x", "wheel", 2000);
        assertNull(h.redo("x"));
    }

    @Test
    void readableNames() {
        assertEquals("Options", ScreenNames.readable("net.minecraft.client.gui.screens.options.OptionsScreen"));
        assertEquals("Video Settings", ScreenNames.readable("a.b.VideoSettingsScreen"));
        assertEquals("Creative Mode Inventory", ScreenNames.readable("x.CreativeModeInventoryScreen"));
        assertEquals("GUI Thing", ScreenNames.readable("x.Outer$GUIThing"));
        assertEquals("Screen", ScreenNames.readable("Screen"));
    }

    @Test
    void blocklist() {
        Blocklist b = new Blocklist("com.example.Big*, Fancy");
        assertTrue(b.blocked("dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen"));
        assertTrue(b.blocked("dev.henny.vui.mc.VuiScreen"));
        assertTrue(b.blocked("net.minecraft.client.gui.screens.ChatScreen"));
        assertTrue(b.blocked("com.example.BigScreen"));
        assertTrue(b.blocked("org.mod.Fancy"));
        assertFalse(b.blocked("net.minecraft.client.gui.screens.options.OptionsScreen"));
        assertTrue(b.blocked(null));
    }

    @Test
    void hitPrefersSmallestAndFindsEdges() {
        List<Box> boxes = List.of(new Box(0, 0, 200, 100), new Box(50, 40, 60, 20));
        Hit.Result r = Hit.find(boxes, List.of(false, true), 80, 50);
        assertEquals(1, r.index());
        assertEquals(Hit.Zone.BODY, r.zone());
        assertEquals(Hit.Zone.RIGHT, Hit.find(boxes, List.of(false, true), 109, 50).zone());
        assertEquals(Hit.Zone.LEFT, Hit.find(boxes, List.of(false, true), 51, 50).zone());
        assertEquals(Hit.Zone.BODY, Hit.find(boxes, List.of(false, false), 51, 50).zone());
        assertEquals(-1, Hit.find(boxes, List.of(false, true), 300, 50).index());
    }

    @Test
    void spotsAvoidTakenCorners() {
        Box first = Spots.pick(400, 300, 14, List.of());
        assertEquals(400 - Spots.MARGIN - 14, first.x(), 0.01);
        Box second = Spots.pick(400, 300, 14, List.of(new Box(300, 280, 100, 20)));
        assertEquals(Spots.MARGIN, second.x(), 0.01);
    }
}

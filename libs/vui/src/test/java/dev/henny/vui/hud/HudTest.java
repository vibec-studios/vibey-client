package dev.henny.vui.hud;

import dev.henny.vui.Vui;
import dev.henny.vui.CountingCanvas;
import dev.henny.vui.dom.Document;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.headless.Headless;
import dev.henny.vui.layout.Anchor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudTest {
    private static Document widget(Vui vui, String text) {
        return vui.fromSource("w.vui", "<template><row class=\"w\"><text>" + text + "</text></row></template><style>.w { padding: 2; }</style>", null);
    }

    private static HudLayer layer() {
        Vui vui = Headless.create().vui();
        HudLayer layer = new HudLayer();
        layer.add("fps", widget(vui, "FPS 60"), HudPlacement.at(Anchor.TOP_LEFT, 4, 4));
        layer.add("coords", widget(vui, "XYZ"), HudPlacement.at(Anchor.BOTTOM_RIGHT, -4, -4));
        layer.resize(320, 180);
        return layer;
    }

    @Test
    void anchoredPlacementUsesMeasuredContent() {
        HudLayer l = layer();
        HudWidget fps = l.widget("fps");
        assertEquals(new Rect(4, 4, 40, 13), fps.bounds());
        Rect c = l.widget("coords").bounds();
        assertEquals(320 - 4 - 22, c.x(), 0.01);
        assertEquals(180 - 4 - 13, c.y(), 0.01);
    }

    @Test
    void placementStaysOnScreen() {
        HudPlacement p = new HudPlacement(Anchor.TOP_LEFT, 500, -50, 1f, true);
        Rect r = p.rect(40, 10, 320, 180);
        assertEquals(280, r.x(), 0.01);
        assertEquals(0, r.y(), 0.01);
    }

    @Test
    void editorDragsSnapsAndReanchors() {
        HudLayer l = layer();
        HudEditor e = l.editor();
        e.active(true);
        assertTrue(e.pointerDown(10, 10));
        e.pointerMove(160 - 20 + 6 + 2, 90);
        Rect b = l.widget("fps").bounds();
        assertEquals(140, b.x(), 0.01);
        assertFalse(e.guides().isEmpty());
        e.pointerUp(0, 0);
        assertEquals(Anchor.CENTER, l.widget("fps").placement().anchor());
        e.pointerDown(150, 90);
        e.pointerMove(400, 400);
        Rect clamped = l.widget("fps").bounds();
        assertEquals(320 - 40, clamped.x(), 0.01);
        assertEquals(180 - 13, clamped.y(), 0.01);
        assertEquals(Anchor.BOTTOM_RIGHT, l.widget("fps").placement().anchor());
    }

    @Test
    void wheelResizesAroundCenter() {
        HudLayer l = layer();
        HudEditor e = l.editor();
        e.active(true);
        Rect before = l.widget("fps").bounds();
        e.scroll(10, 10, 1);
        HudWidget fps = l.widget("fps");
        assertEquals(1.1f, fps.placement().scale(), 1e-4);
        assertEquals(before.width() * 1.1f, fps.bounds().width(), 0.01);
        for (int i = 0; i < 100; i++) e.scroll(fps.bounds().centerX(), fps.bounds().centerY(), -1);
        assertEquals(HudPlacement.MIN_SCALE, fps.placement().scale(), 1e-4);
    }

    @Test
    void layoutRoundTripsThroughJson() {
        HudLayer l = layer();
        l.widget("fps").placement(new HudPlacement(Anchor.RIGHT, -10, 3, 1.5f, false));
        String json = l.toJson();
        HudLayer other = layer();
        other.fromJson(json);
        HudPlacement p = other.widget("fps").placement();
        assertEquals(Anchor.RIGHT, p.anchor());
        assertEquals(-10f, p.offsetX());
        assertEquals(1.5f, p.scale());
        assertFalse(p.visible());
        other.fromJson("{ broken");
        assertEquals(Anchor.RIGHT, other.widget("fps").placement().anchor());
    }

    @Test
    void snapEngineSnapsToCenterAndEdges() {
        SnapEngine.Result r = SnapEngine.snap(new Rect(138, 50, 40, 10), List.of(), 320, 180, 4);
        assertEquals(140, r.rect().x(), 0.01);
        SnapEngine.Result edge = SnapEngine.snap(new Rect(2, 50, 40, 10), List.of(new Rect(100, 0, 10, 10)), 320, 180, 4);
        assertEquals(0, edge.rect().x(), 0.01);
    }

    @Test
    void rendersWidgetsAndEditorOverlay() {
        HudLayer l = layer();
        CountingCanvas c = new CountingCanvas();
        l.render(c);
        long normal = c.total();
        assertTrue(normal > 0);
        l.editor().active(true);
        CountingCanvas c2 = new CountingCanvas();
        l.render(c2);
        assertTrue(c2.fills > c.fills);
        assertEquals(0, c2.depth());
        assertSame(l.widget("fps"), l.widgetAt(5, 5));
    }
}

package dev.henny.r3.interact;

import dev.henny.r3.input.InputCode;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.support.Rig;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResizeTest {
    private static final float ASPECT = 16f / 9f;
    private final Rig rig = new Rig();
    private final WorldObject screen = rig.wallScreen();

    private double leftEdgeX() {
        Transform t = screen.transform();
        return t.position().x - t.right(new Vector3d()).x * t.worldWidth() / 2;
    }

    @Test
    void wheelOnTheBodyResizesUniformly() {
        rig.lookAt(0.5, 65, 4.98).frame();
        assertTrue(rig.scene.wheel(1));
        assertEquals(2.25f, screen.transform().worldWidth(), 1e-5);
        assertEquals(2.25f / ASPECT, screen.transform().worldHeight(), 1e-4);
        assertEquals(0.5, screen.transform().position().x, 1e-9, "around the center");
        assertEquals(InteractionEventKind.RESIZED, rig.last().kind());
    }

    @Test
    void wheelStopsAtTheLimits() {
        rig.lookAt(0.5, 65, 4.98).frame();
        for (int i = 0; i < 100; i++) rig.scene.wheel(1);
        assertEquals(16f, screen.transform().worldWidth(), 1e-4);
        for (int i = 0; i < 200; i++) rig.scene.wheel(-1);
        assertEquals(0.25f, screen.transform().worldHeight(), 1e-4);
        assertEquals(0.25f * ASPECT, screen.transform().worldWidth(), 1e-4);
    }

    @Test
    void fineStepWithModifier() {
        rig.lookAt(0.5, 65, 4.98).frame();
        rig.keys.hold(InputCode.KEY_ALT);
        rig.scene.wheel(1);
        assertEquals(2.0625f, screen.transform().worldWidth(), 1e-5);
    }

    @Test
    void draggingAnEdgeKeepsTheOppositeEdgeInPlace() {
        double left = leftEdgeX();
        rig.lookAt(-0.5, 65, 4.98).frame();
        assertTrue(rig.press(InputCode.MOUSE_MIDDLE));
        assertEquals(InteractionState.RESIZING, rig.interaction.state());
        rig.lookAt(-1.5, 65, 4.98).frame();
        assertEquals(3f, screen.transform().worldWidth(), 1e-3);
        assertEquals(3f / ASPECT, screen.transform().worldHeight(), 1e-3);
        assertEquals(left, leftEdgeX(), 1e-4);
        assertTrue(rig.release(InputCode.MOUSE_MIDDLE));
        assertEquals(InteractionEventKind.RESIZED, rig.last().kind());
        assertTrue(rig.interaction.undo());
        assertEquals(2f, screen.transform().worldWidth(), 1e-5);
    }

    @Test
    void freeAspectCornerDragKeepsTheOppositeCorner() {
        screen.rules(new InteractionRules());
        rig.lookAt(-0.5, 65.5625, 4.98).frame();
        assertEquals(HoverZone.CORNER, rig.interaction.hover().zone());
        rig.press(InputCode.MOUSE_MIDDLE);
        rig.lookAt(-1.0, 66.0, 4.98).frame();
        Transform t = screen.transform();
        assertEquals(2.5f, t.worldWidth(), 1e-3);
        assertEquals(1.5625f, t.worldHeight(), 1e-3);
        assertEquals(1.5, leftEdgeX(), 1e-4);
        assertEquals(64.4375, t.position().y - t.worldHeight() / 2, 1e-4);
    }

    @Test
    void keepAspectCornerUsesTheBiggerChange() {
        rig.lookAt(-0.5, 65.5625, 4.98).frame();
        rig.press(InputCode.MOUSE_MIDDLE);
        rig.lookAt(-1.0, 66.5, 4.98).frame();
        Transform t = screen.transform();
        assertEquals(ASPECT, t.worldWidth() / t.worldHeight(), 1e-3);
        assertEquals(64.4375, t.position().y - t.worldHeight() / 2, 1e-4);
        assertEquals(1.5, leftEdgeX(), 1e-4);
    }

    @Test
    void edgeDragRespectsMinimumSize() {
        screen.rules(new InteractionRules());
        rig.lookAt(-0.5, 65, 4.98).frame();
        rig.press(InputCode.MOUSE_MIDDLE);
        rig.lookAt(3.0, 65, 4.98).frame();
        assertEquals(0.25f, screen.transform().worldWidth(), 1e-4);
        assertEquals(1.5, leftEdgeX(), 1e-4);
    }

    @Test
    void wheelOnAnEdgeGrowsThatEdge() {
        double left = leftEdgeX();
        rig.lookAt(-0.5, 65, 4.98).frame();
        rig.scene.wheel(1);
        assertEquals(2.125f, screen.transform().worldWidth(), 1e-4);
        assertEquals(left, leftEdgeX(), 1e-4);
    }

    @Test
    void escapeCancelsAnEdgeDrag() {
        rig.lookAt(-0.5, 65, 4.98).frame();
        rig.press(InputCode.MOUSE_MIDDLE);
        rig.lookAt(-1.5, 65, 4.98).frame();
        assertTrue(rig.press(InputCode.KEY_ESCAPE));
        assertEquals(2f, screen.transform().worldWidth(), 1e-5);
        assertEquals(InteractionEventKind.RESIZE_CANCELLED, rig.last().kind());
    }

    @Test
    void lockedObjectsIgnoreTheWheel() {
        screen.locked(true);
        rig.lookAt(0.5, 65, 4.98).frame();
        assertFalse(rig.scene.wheel(1));
        assertEquals(2f, screen.transform().worldWidth(), 1e-6);
        assertFalse(rig.scene.blocksHotbarScroll());
    }

    @Test
    void wheelStepsInARowAreOneUndo() {
        rig.lookAt(0.5, 65, 4.98).frame();
        rig.scene.wheel(1);
        rig.frames(10);
        rig.scene.wheel(1);
        rig.scene.wheel(1);
        assertEquals(1, rig.interaction.history().size());
        rig.frames(90);
        rig.scene.wheel(1);
        assertEquals(2, rig.interaction.history().size());
        rig.keys.hold(InputCode.KEY_CONTROL);
        assertTrue(rig.press(InputCode.KEY_Z));
        assertEquals(2.75f, screen.transform().worldWidth(), 1e-4);
        assertTrue(rig.press(InputCode.KEY_Z));
        assertEquals(2f, screen.transform().worldWidth(), 1e-4);
        assertFalse(rig.press(InputCode.KEY_Z));
    }

    @Test
    void zWithoutControlDoesNotUndo() {
        rig.lookAt(0.5, 65, 4.98).frame();
        rig.scene.wheel(1);
        assertFalse(rig.press(InputCode.KEY_Z));
        assertEquals(1, rig.interaction.history().size());
    }
}

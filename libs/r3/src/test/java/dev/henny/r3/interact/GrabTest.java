package dev.henny.r3.interact;

import dev.henny.r3.anchor.AnchorKind;
import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.input.InputCode;
import dev.henny.r3.math.Face;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.support.Rig;
import dev.henny.r3.support.TestPanel;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrabTest {
    private final Rig rig = new Rig();
    private final WorldObject screen = rig.wallScreen();

    private void grab() {
        rig.lookAt(0.5, 65, 4.98).frame();
        assertTrue(rig.press(InputCode.MOUSE_MIDDLE));
        assertEquals(InteractionState.HOLDING, rig.interaction.state());
    }

    @Test
    void holdingMiddleMouseGrabsAndSlidesAlongTheWall() {
        grab();
        assertEquals(1, rig.count(InteractionEventKind.GRAB_STARTED));
        rig.lookAt(-2.5, 66, 5).frame();
        Vector3d p = screen.transform().position();
        assertEquals(4.98, p.z, 1e-9);
        assertEquals(-2.5, p.x, 0.07);
        assertEquals(66, p.y, 0.07);
        assertEquals(AnchorKind.BLOCK_FACE, screen.anchor().kind());
    }

    @Test
    void awayFromWallsItFloatsAtTheSameDistanceFacingYou() {
        grab();
        rig.look(180, 0).frame();
        Vector3d p = screen.transform().position();
        assertEquals(0.5 - 4.48, p.z, 0.01);
        assertEquals(1, screen.transform().normal(new Vector3d()).z, 1e-4);
        assertEquals(AnchorKind.FREE, screen.anchor().kind());
    }

    @Test
    void releasingPlacesAndIsUndoable() {
        grab();
        rig.lookAt(-2.5, 66, 5).frame();
        assertTrue(rig.release(InputCode.MOUSE_MIDDLE));
        assertEquals(InteractionState.IDLE, rig.interaction.state());
        assertEquals(InteractionEventKind.MOVED, rig.last().kind());
        assertEquals(1, rig.interaction.history().size());
        assertTrue(rig.interaction.undo());
        assertEquals(0.5, screen.transform().position().x, 1e-6);
        assertEquals(InteractionEventKind.UNDONE, rig.last().kind());
    }

    @Test
    void escapeCancelsAndRestores() {
        grab();
        rig.look(180, 0).frame();
        assertTrue(rig.press(InputCode.KEY_ESCAPE));
        assertEquals(InteractionState.IDLE, rig.interaction.state());
        assertEquals(4.98, screen.transform().position().z, 1e-9);
        assertEquals(AnchorKind.BLOCK_FACE, screen.anchor().kind());
        assertEquals(InteractionEventKind.MOVE_CANCELLED, rig.last().kind());
        assertFalse(rig.release(InputCode.MOUSE_MIDDLE));
        assertEquals(0, rig.interaction.history().size());
    }

    @Test
    void rightClickWhileHoldingCancelsToo() {
        grab();
        rig.look(180, 0).frame();
        assertTrue(rig.press(InputCode.MOUSE_RIGHT));
        assertEquals(4.98, screen.transform().position().z, 1e-9);
        assertEquals(0, rig.count(InteractionEventKind.CONTEXT_MENU));
    }

    @Test
    void shiftWheelChangesTheDistanceAndStopsSnapping() {
        grab();
        rig.keys.hold(InputCode.KEY_SHIFT);
        assertTrue(rig.scene.wheel(-4));
        rig.frame();
        assertEquals(2.48, rig.interaction.grab().holdDistance(), 1e-6);
        assertEquals(AnchorKind.FREE, screen.anchor().kind());
        assertEquals(0.5 + 2.48, screen.transform().position().z, 0.01);
        assertEquals(2f, screen.transform().worldWidth(), 1e-6, "no resize while shift is down");
    }

    @Test
    void wheelWhileHoldingResizes() {
        grab();
        rig.scene.wheel(2);
        assertEquals(2.5f, screen.transform().worldWidth(), 1e-5);
        assertEquals(2.5f / (16f / 9f), screen.transform().worldHeight(), 1e-4);
    }

    @Test
    void floorScreensSnapFlatAndTurnWithR() {
        rig.world.floor(63, -8, 8, -8, 4);
        grab();
        rig.look(0, 70).frame();
        assertEquals(Face.UP, ((BlockFaceAnchor) screen.anchor()).face());
        assertEquals(Face.SOUTH, ((BlockFaceAnchor) screen.anchor()).up());
        assertEquals(64.02, screen.transform().position().y, 1e-6);
        assertTrue(rig.press(InputCode.KEY_R));
        rig.frame();
        assertEquals(Face.WEST, ((BlockFaceAnchor) screen.anchor()).up());
        assertEquals(-1, screen.transform().up(new Vector3d()).x, 1e-5);
    }

    @Test
    void lockedObjectsCannotBeGrabbed() {
        screen.locked(true);
        rig.lookAt(0.5, 65, 4.98).frame();
        assertFalse(rig.press(InputCode.MOUSE_MIDDLE));
        assertEquals(InteractionState.IDLE, rig.interaction.state());
        screen.locked(false).remote(true);
        assertFalse(rig.press(InputCode.MOUSE_MIDDLE));
    }

    @Test
    void walkingKeysPassThroughButMouseIsSwallowedWhileHolding() {
        grab();
        assertFalse(rig.press(InputCode.key('W')));
        assertTrue(rig.press(InputCode.MOUSE_LEFT));
        assertTrue(rig.interaction.capturing());
        assertTrue(rig.scene.blocksHotbarScroll());
    }

    @Test
    void newObjectsArePlacedOnTheSurfaceYouLookAt() {
        rig.camera.position(0.5, 65, -10);
        WorldObject fresh = rig.scene.add(WorldObject.create("test:new").content(new TestPanel()));
        fresh.transform().size(1.6f, 0.9f);
        rig.interaction.beginPlacement(fresh);
        rig.look(0, 0).frame();
        assertEquals(4.98, fresh.transform().position().z, 1e-9);
        assertTrue(rig.press(InputCode.MOUSE_LEFT));
        assertEquals(InteractionEventKind.PLACED, rig.last().kind());
        assertSame(fresh, rig.last().target());
        assertEquals(0, rig.interaction.history().size());
    }

    @Test
    void cancellingANewPlacementSaysSo() {
        WorldObject fresh = rig.scene.add(WorldObject.create("test:new").content(new TestPanel()));
        rig.interaction.beginPlacement(fresh);
        rig.frame();
        rig.press(InputCode.MOUSE_RIGHT);
        assertEquals(InteractionEventKind.PLACEMENT_CANCELLED, rig.last().kind());
    }

    @Test
    void moveFromAMenuPlacesWithAClick() {
        rig.frame();
        assertTrue(rig.interaction.beginMove(screen));
        rig.lookAt(3, 66, 5).frame();
        rig.release(InputCode.MOUSE_MIDDLE);
        assertEquals(InteractionState.HOLDING, rig.interaction.state());
        rig.press(InputCode.MOUSE_LEFT);
        assertEquals(InteractionEventKind.MOVED, rig.last().kind());
        assertEquals(3, screen.transform().position().x, 0.07);
    }

    @Test
    void removingTheHeldObjectCancelsTheGrab() {
        grab();
        rig.scene.remove(screen.id());
        assertEquals(InteractionState.IDLE, rig.interaction.state());
    }
}

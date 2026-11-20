package dev.henny.r3.interact;

import dev.henny.r3.input.InputCode;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.support.Rig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlsTest {
    private final Rig rig = new Rig();
    private final WorldObject screen = rig.wallScreen();

    ControlsTest() {
        rig.panel.control("play", 0, 50, 32, 22);
    }

    @Test
    void clickingAControlFiresItsAction() {
        rig.lookAt(1.3, 64.606, 4.98).frame();
        assertTrue(rig.press(InputCode.MOUSE_LEFT));
        InteractionEvent e = rig.last();
        assertEquals(InteractionEventKind.ACTION, e.kind());
        assertEquals("play", e.controlId());
        assertSame(screen, e.target());
        assertEquals(InteractionState.CONTENT_DRAG, rig.interaction.state());
        assertTrue(rig.release(InputCode.MOUSE_LEFT));
        assertEquals(List.of("hover:play", "down:play", "up"), rig.panel.log);
        assertEquals(InteractionState.IDLE, rig.interaction.state());
    }

    @Test
    void clickOnTheBodyIsSwallowedButFiresNothing() {
        rig.lookAt(0.5, 65, 4.98).frame();
        assertTrue(rig.press(InputCode.MOUSE_LEFT));
        assertEquals(0, rig.count(InteractionEventKind.ACTION));
        assertEquals(InteractionState.IDLE, rig.interaction.state());
    }

    @Test
    void clicksElsewhereGoToVanilla() {
        rig.look(180, 0).frame();
        assertFalse(rig.press(InputCode.MOUSE_LEFT));
        assertFalse(rig.press(InputCode.MOUSE_RIGHT));
        assertFalse(rig.scene.wheel(1));
        assertFalse(rig.scene.blocksVanillaMouse());
    }

    @Test
    void rightClickAsksForTheContextMenu() {
        rig.lookAt(0.5, 65, 4.98).frame();
        assertTrue(rig.press(InputCode.MOUSE_RIGHT));
        assertEquals(InteractionEventKind.CONTEXT_MENU, rig.last().kind());
        assertTrue(rig.scene.blocksVanillaMouse());
    }

    @Test
    void contentCanDragLikeASlider() {
        rig.panel.dragsOnBody = true;
        rig.lookAt(0.5, 65, 4.98).frame();
        rig.press(InputCode.MOUSE_LEFT);
        assertEquals(InteractionState.CONTENT_DRAG, rig.interaction.state());
        rig.lookAt(-1.5, 65, 4.98).frame();
        assertTrue(rig.panel.lastX > 128, "pointer keeps moving past the panel edge");
        rig.release(InputCode.MOUSE_LEFT);
        assertTrue(rig.panel.log.contains("up"));
    }

    @Test
    void contentScrollWinsOverResize() {
        rig.panel.scrolls = true;
        rig.lookAt(0.5, 65, 4.98).frame();
        assertTrue(rig.scene.wheel(1));
        assertEquals(2f, screen.transform().worldWidth(), 1e-6);
        assertTrue(rig.panel.log.contains("scroll:1.0"));
    }

    @Test
    void lockedObjectsStillWork() {
        screen.locked(true);
        rig.lookAt(1.3, 64.606, 4.98).frame();
        rig.press(InputCode.MOUSE_LEFT);
        assertEquals(InteractionEventKind.ACTION, rig.last().kind());
        rig.release(InputCode.MOUSE_LEFT);
        rig.press(InputCode.MOUSE_RIGHT);
        assertEquals(InteractionEventKind.CONTEXT_MENU, rig.last().kind());
    }

    @Test
    void controlsAreNotHoveredWhileHolding() {
        rig.lookAt(0.5, 65, 4.98).frame();
        rig.press(InputCode.MOUSE_MIDDLE);
        rig.lookAt(1.3, 64.606, 4.98).frame();
        assertEquals(0, rig.panel.log.stream().filter(s -> s.equals("hover:play")).count());
    }
}

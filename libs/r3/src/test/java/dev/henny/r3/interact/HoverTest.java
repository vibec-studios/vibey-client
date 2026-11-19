package dev.henny.r3.interact;

import dev.henny.r3.object.WorldObject;
import dev.henny.r3.support.Rig;
import dev.henny.r3.support.TestPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoverTest {
    private final Rig rig = new Rig();
    private final WorldObject screen = rig.wallScreen();

    @Test
    void lookingAtTheMiddleHoversTheBody() {
        rig.lookAt(0.5, 65, 4.98).frame();
        HoverState h = rig.interaction.hover();
        assertSame(screen, h.target());
        assertEquals(HoverZone.BODY, h.zone());
        assertEquals(64f, h.pixelX(), 0.5f);
        assertEquals(36f, h.pixelY(), 0.5f);
    }

    @Test
    void controlsAreFoundInPanelPixels() {
        rig.panel.control("play", 0, 50, 32, 22);
        rig.lookAt(1.3, 64.606, 4.98).frame();
        assertEquals(HoverZone.CONTROL, rig.interaction.hover().zone());
        assertEquals("play", rig.interaction.hover().controlId());
        assertEquals("play", rig.panel.hovered);
        rig.lookAt(0.5, 65, 4.98).frame();
        assertNull(rig.panel.hovered);
    }

    @Test
    void lookingAtTheBorderShowsTheEdgeHandle() {
        rig.lookAt(-0.5, 65, 4.98).frame();
        assertEquals(HoverZone.EDGE, rig.interaction.hover().zone());
        assertEquals(ResizeHandle.RIGHT, rig.interaction.hover().handle());
        rig.lookAt(-0.53, 65, 4.98).frame();
        assertEquals(ResizeHandle.RIGHT, rig.interaction.hover().handle(), "the zone reaches a bit outside");
        rig.lookAt(1.5, 65, 4.98).frame();
        assertEquals(ResizeHandle.LEFT, rig.interaction.hover().handle());
    }

    @Test
    void cornersAreTheirOwnZone() {
        rig.lookAt(-0.5, 65.5625, 4.98).frame();
        assertEquals(HoverZone.CORNER, rig.interaction.hover().zone());
        assertEquals(ResizeHandle.TOP_RIGHT, rig.interaction.hover().handle());
        rig.lookAt(1.5, 64.4375, 4.98).frame();
        assertEquals(ResizeHandle.BOTTOM_LEFT, rig.interaction.hover().handle());
    }

    @Test
    void borderGrowsWithDistanceSoItStaysGrabbable() {
        InteractionConfig c = InteractionConfig.defaults();
        double near = BorderZone.width(2, 1, 0.56, c);
        double far = BorderZone.width(12, 1, 0.56, c);
        assertTrue(far > near * 3);
        assertEquals(c.borderMin, BorderZone.width(0.1, 1, 0.56, c), 1e-9);
        assertTrue(BorderZone.width(1000, 1, 0.56, c) <= 0.56 * 2 * c.borderMaxFraction + 1e-9);
    }

    @Test
    void lockedObjectsHaveNoResizeBorder() {
        screen.locked(true);
        rig.lookAt(-0.53, 65, 4.98).frame();
        assertNull(rig.interaction.hover().target());
        rig.lookAt(-0.49, 65, 4.98).frame();
        assertEquals(HoverZone.BODY, rig.interaction.hover().zone());
    }

    @Test
    void nearestObjectWins() {
        WorldObject front = WorldObject.create("test:front").content(new TestPanel());
        front.transform().size(1, 1).position(0.5, 65, 3);
        front.transform().orientation().set(screen.transform().orientation());
        rig.scene.add(front);
        rig.lookAt(0.5, 65, 4.98).frame();
        assertSame(front, rig.interaction.hover().target());
    }

    @Test
    void blocksInFrontHideTheObject() {
        rig.world.block(0, 65, 2);
        rig.lookAt(0.5, 65, 4.98).frame();
        assertNull(rig.interaction.hover().target());
    }

    @Test
    void otherDimensionsAreIgnored() {
        screen.dimension("minecraft:the_nether");
        rig.lookAt(0.5, 65, 4.98).frame();
        assertNull(rig.interaction.hover().target());
        assertTrue(!screen.state().visible());
    }

    @Test
    void lookingAwayExitsTheContent() {
        rig.lookAt(0.5, 65, 4.98).frame();
        rig.look(180, 0).frame();
        assertNull(rig.interaction.hover().target());
        assertTrue(rig.panel.log.contains("exit"));
    }
}

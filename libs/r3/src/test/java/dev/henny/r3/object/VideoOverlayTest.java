package dev.henny.r3.object;

import dev.henny.r3.input.InputCode;
import dev.henny.r3.interact.HoverZone;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.math.Face;
import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.anchor.FaceSnap;
import dev.henny.r3.support.RecordingCanvas;
import dev.henny.r3.support.Rig;
import dev.henny.r3.support.TestPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoOverlayTest {
    final Rig rig = new Rig();
    final TestPanel strip = new TestPanel();
    final VideoSurface surface = new VideoSurface().overlay(strip);

    WorldObject screen() {
        WorldObject o = WorldObject.create("test:video").content(surface).rules(InteractionRules.screen());
        o.dimension(rig.world.dimension);
        o.transform().size(2f, 1.125f).position(0.5, 65, 5 - FaceSnap.DEFAULT_OFFSET);
        FaceSnap.orientation(Face.NORTH, Face.UP, o.transform().orientation());
        o.anchor(new BlockFaceAnchor(Face.NORTH, Face.UP, 0, 65, 5));
        return rig.scene.add(o);
    }

    @Test
    void controlsAppearWhileLookingAndLingerAfterwards() {
        screen();
        strip.control("play", 0, 0, 2000, 2000);
        rig.lookAt(0.5, 65, 5).frames(2);
        assertTrue(surface.overlayShown());
        assertEquals(HoverZone.CONTROL, rig.interaction.hover().zone());
        rig.look(180, 0).frame();
        assertTrue(surface.overlayShown(), "still there right after looking away");
        rig.frames(Math.round(VideoOverlay.DEFAULT_LINGER * 60) + 5);
        assertFalse(surface.overlayShown());
    }

    @Test
    void hiddenOverlayHasNoControlsSoTheBodyCanBeGrabbed() {
        screen();
        strip.control("play", 0, 0, 2000, 2000);
        assertEquals(null, surface.controlAt(10, 10));
        rig.lookAt(0.5, 65, 5).frames(2);
        assertEquals("play", surface.controlAt(10, 10));
    }

    @Test
    void clickReachesTheStrip() {
        screen();
        strip.control("play", 0, 0, 2000, 2000);
        rig.lookAt(0.5, 65, 5).frames(2);
        rig.click(InputCode.mouse(0));
        assertTrue(strip.log.stream().anyMatch(s -> s.startsWith("down")), strip.log.toString());
    }

    @Test
    void overlayIsDrawnInFrontOfThePicture() {
        screen();
        rig.lookAt(0.5, 65, 5).frames(2);
        RecordingCanvas canvas = new RecordingCanvas();
        rig.scene.render(canvas);
        assertTrue(canvas.panels > 0, "the strip is drawn as a panel");
    }
}

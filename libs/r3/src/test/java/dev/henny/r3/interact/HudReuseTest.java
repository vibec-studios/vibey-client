package dev.henny.r3.interact;

import dev.henny.r3.hint.SetHintStore;
import dev.henny.r3.input.GestureBindings;
import dev.henny.r3.input.InputCode;
import dev.henny.r3.math.Axes;
import dev.henny.r3.object.Transform;
import dev.henny.r3.support.TestKeys;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudReuseTest {
    static final class HudBox implements Interactable {
        final Transform transform = new Transform();
        final InteractionRules rules = InteractionRules.hud();

        HudBox(double x, double y, float w, float h) {
            transform.position(x, y, 0).size(w, h);
            Axes.fromAxes(new Vector3d(1, 0, 0), new Vector3d(0, -1, 0), new Vector3d(0, 0, -1), transform.orientation());
        }

        @Override
        public Transform transform() {
            return transform;
        }

        @Override
        public InteractionRules rules() {
            return rules;
        }

        @Override
        public boolean locked() {
            return false;
        }

        @Override
        public int pixelWidth() {
            return Math.round(transform.width());
        }

        @Override
        public int pixelHeight() {
            return Math.round(transform.height());
        }
    }

    private final TestKeys keys = new TestKeys();
    private final Interaction interaction = new Interaction(InteractionConfig.hud(), GestureBindings.defaults(), keys, new SetHintStore());
    private final HudBox box = new HudBox(100, 50, 80, 40);
    private final Pointer pointer = new Pointer();

    private void mouse(double x, double y) {
        interaction.update(pointer.fromScreenPoint(x, y), List.of(box), 0.016f);
    }

    @Test
    void screenCoordinatesMapToPanelPixels() {
        mouse(70, 35);
        assertSame(box, interaction.hover().target());
        assertEquals(10f, interaction.hover().pixelX(), 1e-3);
        assertEquals(5f, interaction.hover().pixelY(), 1e-3);
    }

    @Test
    void dragMovesTheWidgetWithTheMouse() {
        mouse(100, 50);
        assertTrue(interaction.press(InputCode.MOUSE_MIDDLE));
        mouse(150, 70);
        assertEquals(150, box.transform.position().x, 1e-6);
        assertEquals(70, box.transform.position().y, 1e-6);
        interaction.release(InputCode.MOUSE_MIDDLE);
        assertEquals(InteractionState.IDLE, interaction.state());
        assertTrue(interaction.undo());
        assertEquals(100, box.transform.position().x, 1e-6);
    }

    @Test
    void edgesWorkInScreenSpace() {
        mouse(139, 50);
        assertEquals(ResizeHandle.RIGHT, interaction.hover().handle());
        interaction.press(InputCode.MOUSE_MIDDLE);
        mouse(160, 50);
        assertEquals(101, box.transform.worldWidth(), 1e-3);
        assertEquals(60, box.transform.position().x - box.transform.worldWidth() / 2, 1e-3);
    }

    @Test
    void wheelScalesInPixels() {
        mouse(100, 50);
        interaction.wheel(1);
        assertEquals(88, box.transform.worldWidth(), 1e-3);
    }
}

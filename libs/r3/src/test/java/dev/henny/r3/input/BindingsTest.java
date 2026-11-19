package dev.henny.r3.input;

import dev.henny.r3.interact.InteractionState;
import dev.henny.r3.support.Rig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingsTest {
    @Test
    void reboundGrabUsesTheNewButton() {
        Rig rig = new Rig();
        rig.wallScreen();
        rig.interaction.bindings().set(Gesture.GRAB, Binding.of(InputCode.mouse(3)));
        rig.lookAt(0.5, 65, 4.98).frame();
        assertFalse(rig.press(InputCode.MOUSE_MIDDLE));
        assertTrue(rig.press(InputCode.mouse(3)));
        assertEquals(InteractionState.HOLDING, rig.interaction.state());
        assertTrue(rig.release(InputCode.mouse(3)));
        assertEquals(InteractionState.IDLE, rig.interaction.state());
    }

    @Test
    void bindingsRoundTripThroughAMap() {
        GestureBindings b = GestureBindings.defaults().set(Gesture.ROTATE, Binding.of(InputCode.key('T')));
        Map<String, String> saved = b.toMap();
        assertEquals("key:341+key:90", saved.get("UNDO"));
        GestureBindings loaded = new GestureBindings().load(saved);
        assertEquals(InputCode.key('T'), loaded.get(Gesture.ROTATE).code());
        assertEquals(InputCode.KEY_CONTROL, loaded.get(Gesture.UNDO).modifier());
        assertEquals("Ctrl+Z", loaded.displayName(Gesture.UNDO));
    }

    @Test
    void badEntriesAreIgnored() {
        GestureBindings b = new GestureBindings().load(Map.of("GRAB", "mouse:x", "NOPE", "key:1", "MENU", "joystick:1"));
        assertEquals(InputCode.MOUSE_MIDDLE, b.get(Gesture.GRAB).code());
        assertEquals(InputCode.MOUSE_RIGHT, b.get(Gesture.MENU).code());
    }

    @Test
    void wheelResizeIsNotRebindable() {
        GestureBindings b = new GestureBindings().set(Gesture.RESIZE, Binding.of(InputCode.KEY_R));
        assertEquals(InputCode.WHEEL, b.get(Gesture.RESIZE).code());
    }

    @Test
    void inputCodesAreValuesWithNames() {
        assertSame(InputCode.KEY_R, InputCode.key(82));
        assertEquals(InputCode.mouse(7), InputCode.mouse(7));
        assertEquals("Mouse 8", InputCode.mouse(7).displayName());
        assertEquals(InputCode.MOUSE_LEFT, InputCode.parse("mouse:0"));
        assertNull(InputCode.parse("pad:1"));
        assertTrue(Binding.with(InputCode.KEY_CONTROL, InputCode.KEY_Z)
                .matches(InputCode.KEY_Z, code -> code.equals(InputCode.KEY_CONTROL)));
    }
}

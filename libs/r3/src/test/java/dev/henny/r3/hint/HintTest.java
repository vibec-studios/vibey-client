package dev.henny.r3.hint;

import dev.henny.r3.input.Binding;
import dev.henny.r3.input.Gesture;
import dev.henny.r3.input.InputCode;
import dev.henny.r3.support.Rig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HintTest {
    private final Rig rig = new Rig();

    HintTest() {
        rig.wallScreen();
    }

    @Test
    void firstHoverShowsOneShortLine() {
        rig.lookAt(0.5, 65, 4.98).frame();
        assertEquals(Hint.HOVER, rig.interaction.hint());
        assertEquals("Middle mouse: move · Wheel: size", rig.interaction.hintText());
    }

    @Test
    void doingTheGestureRetiresTheHintForGood() {
        rig.lookAt(0.5, 65, 4.98).frame();
        rig.press(InputCode.MOUSE_MIDDLE);
        rig.frame();
        assertEquals(Hint.HOLD, rig.interaction.hint());
        rig.release(InputCode.MOUSE_MIDDLE);
        rig.frame();
        assertNull(rig.interaction.hint());
        assertTrue(rig.hints.seen(Hint.HOVER.id()));
        SetHintStore reloaded = SetHintStore.fromJson(rig.hints.toJson());
        assertTrue(reloaded.seen(Hint.HOVER.id()));
        assertTrue(reloaded.seen(Hint.HOLD.id()));
    }

    @Test
    void hintsExpireAfterBeingShownLongEnough() {
        rig.lookAt(0.5, 65, 4.98).frames(30);
        assertEquals(Hint.HOVER, rig.interaction.hint());
        rig.frames(400);
        assertNull(rig.interaction.hint());
    }

    @Test
    void edgeHintOnTheBorder() {
        rig.lookAt(-0.5, 65, 4.98).frame();
        assertEquals(Hint.EDGE, rig.interaction.hint());
    }

    @Test
    void textFollowsRebinding() {
        rig.interaction.bindings().set(Gesture.GRAB, Binding.of(InputCode.mouse(3)));
        rig.lookAt(0.5, 65, 4.98).frame();
        assertEquals("Mouse 4: move · Wheel: size", rig.interaction.hintText());
    }

    @Test
    void storeNotifiesOnChangeAndSurvivesGarbage() {
        int[] changes = {0};
        SetHintStore store = new SetHintStore().onChange(() -> changes[0]++);
        store.markSeen("a");
        store.markSeen("a");
        assertEquals(1, changes[0]);
        assertFalse(SetHintStore.fromJson("{nope").seen("a"));
        assertFalse(SetHintStore.fromJson(null).seen("a"));
    }
}

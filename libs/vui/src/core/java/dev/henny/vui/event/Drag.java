package dev.henny.vui.event;

import dev.henny.vui.style.StateFlags;

final class Drag {
    private Drag() {
    }

    static void move(PointerState state, EventSystem events, float x, float y, float dx, float dy, int mods) {
        if (!state.dragging) {
            float distance = Math.abs(x - state.pressX) + Math.abs(y - state.pressY);
            if (distance < PointerState.DRAG_THRESHOLD) return;
            state.dragging = true;
            state.pressed.state(StateFlags.DRAGGING, true);
            events.send(new PointerEvent().pointer(EventNames.DRAG_START, state.pressed, state.pressX, state.pressY, state.button, mods), true);
        }
        events.send(new PointerEvent().pointer(EventNames.DRAG, state.pressed, x, y, state.button, mods)
                .delta(x - state.pressX, y - state.pressY), true);
    }
}

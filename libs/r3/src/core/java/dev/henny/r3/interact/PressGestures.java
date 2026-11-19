package dev.henny.r3.interact;

import dev.henny.r3.input.Gesture;
import dev.henny.r3.input.InputCode;

final class PressGestures {
    private final Interaction interaction;

    PressGestures(Interaction interaction) {
        this.interaction = interaction;
    }

    boolean handle(InputCode code) {
        return switch (interaction.state()) {
            case HOLDING -> whileHolding(code);
            case RESIZING -> whileResizing(code);
            case CONTENT_DRAG -> true;
            case IDLE -> idle(code);
        };
    }

    private boolean idle(InputCode code) {
        if (interaction.pressed(Gesture.UNDO, code) && interaction.undo()) return true;
        HoverState hover = interaction.hover();
        Interactable t = hover.target();
        if (t == null) return false;
        if (interaction.pressed(Gesture.USE, code)) return interaction.use(t);
        if (interaction.pressed(Gesture.GRAB, code)) return interaction.grabOrResize(t);
        if (interaction.pressed(Gesture.MENU, code)) {
            interaction.emit(InteractionEventKind.CONTEXT_MENU, t, hover.controlId());
            return true;
        }
        return false;
    }

    private boolean whileHolding(InputCode code) {
        if (interaction.pressed(Gesture.CANCEL, code) || interaction.pressed(Gesture.MENU, code)) {
            interaction.cancel();
            return true;
        }
        if (interaction.pressed(Gesture.ROTATE, code)) {
            interaction.grab().rotate();
            return true;
        }
        boolean placeClick = interaction.pressed(Gesture.USE, code) || interaction.pressed(Gesture.GRAB, code);
        if (interaction.grab().clickToPlace() && placeClick) {
            interaction.place();
            return true;
        }
        return code.device() == InputCode.Device.MOUSE;
    }

    private boolean whileResizing(InputCode code) {
        if (interaction.pressed(Gesture.CANCEL, code) || interaction.pressed(Gesture.MENU, code)) interaction.cancel();
        return true;
    }
}

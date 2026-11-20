package dev.henny.r3.interact;

import dev.henny.r3.hint.Hint;
import dev.henny.r3.input.Gesture;
import dev.henny.r3.panel.PointerContent;

final class WheelGestures {
    private final Interaction interaction;

    WheelGestures(Interaction interaction) {
        this.interaction = interaction;
    }

    boolean handle(double notches) {
        if (notches == 0) return false;
        return switch (interaction.state()) {
            case HOLDING -> whileHolding(interaction.grab().target(), notches);
            case RESIZING, CONTENT_DRAG -> true;
            case IDLE -> whileHovering(interaction.hover(), notches);
        };
    }

    private boolean whileHolding(Interactable t, double notches) {
        InteractionRules rules = t.rules();
        if (interaction.held(Gesture.DISTANCE_MODIFIER) && rules.distanceAdjustable()) {
            interaction.grab().adjustDistance(notches);
        } else if (rules.resizable()) {
            UniformResize.step(t.transform(), step(interaction.held(Gesture.FINE_MODIFIER)) * (float) notches, rules.limits());
        }
        interaction.performed(Hint.HOLD);
        return true;
    }

    private boolean whileHovering(HoverState hover, double notches) {
        Interactable t = hover.target();
        if (t == null) return false;
        PointerContent content = t.pointerContent();
        if (content != null && hover.zone().onContent() && content.scroll(hover.pixelX(), hover.pixelY(), notches)) return true;
        if (t.locked() || !t.rules().resizable()) return false;
        boolean fine = interaction.held(Gesture.FINE_MODIFIER) || interaction.held(Gesture.DISTANCE_MODIFIER);
        PoseSnapshot before = PoseSnapshot.capture(t);
        boolean changed;
        if (hover.zone().onBorder() && t.rules().edgeResizable()) {
            float delta = interaction.config().edgeStep * (fine ? interaction.config().fineFactor : 1f) * (float) notches;
            changed = EdgeResize.step(t.transform(), hover.handle(), delta, t.rules());
            interaction.performed(Hint.EDGE);
        } else {
            changed = UniformResize.step(t.transform(), step(fine) * (float) notches, t.rules().limits());
            interaction.performed(Hint.HOVER);
        }
        if (changed) {
            interaction.pushUndo(t, before, UndoKind.WHEEL_RESIZE);
            interaction.emit(InteractionEventKind.RESIZED, t, null);
        }
        return true;
    }

    private float step(boolean fine) {
        InteractionConfig c = interaction.config();
        return fine ? c.wheelStep * c.fineFactor : c.wheelStep;
    }
}

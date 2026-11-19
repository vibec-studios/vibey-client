package dev.henny.r3.interact;

public record InteractionEvent(InteractionEventKind kind, Interactable target, String controlId) {
    public boolean changedPose() {
        return switch (kind) {
            case MOVED, PLACED, RESIZED, UNDONE -> true;
            default -> false;
        };
    }
}

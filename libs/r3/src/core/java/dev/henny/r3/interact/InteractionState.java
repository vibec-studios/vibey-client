package dev.henny.r3.interact;

public enum InteractionState {
    IDLE,
    CONTENT_DRAG,
    HOLDING,
    RESIZING;

    public boolean capturing() {
        return this != IDLE;
    }
}

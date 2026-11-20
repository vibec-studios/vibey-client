package dev.henny.r3.panel;

public interface PointerContent {
    default String controlAt(float x, float y) {
        return null;
    }

    default void hoverChanged(String controlId) {}

    default void pointerMove(float x, float y) {}

    default void pointerExit() {}

    default boolean pointerDown(float x, float y, PointerButton button) {
        return false;
    }

    default void pointerUp(float x, float y, PointerButton button) {}

    default boolean scroll(float x, float y, double amount) {
        return false;
    }
}

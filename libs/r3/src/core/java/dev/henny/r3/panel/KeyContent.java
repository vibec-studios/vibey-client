package dev.henny.r3.panel;

public interface KeyContent {
    boolean keyFocused();

    boolean keyDown(int key, int scancode, int mods, boolean repeat);

    default void keyUp(int key, int scancode, int mods) {}

    boolean charTyped(int codepoint, int mods);

    void releaseKeyFocus();
}

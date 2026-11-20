package dev.henny.vui.render;

public interface Input {
    String clipboard();

    void setClipboard(String text);

    boolean isKeyDown(int key);

    String keyName(int key, int scancode);

    void setCursor(Cursor cursor);

    default String mouseButtonName(int button) {
        return null;
    }
}

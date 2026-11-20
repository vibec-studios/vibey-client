package dev.henny.vui.headless;

import dev.henny.vui.event.Keys;
import dev.henny.vui.render.Cursor;
import dev.henny.vui.render.Input;

import java.util.HashSet;
import java.util.Set;

public final class HeadlessInput implements Input {
    private final Set<Integer> down = new HashSet<>();
    private String clipboard = "";
    private Cursor cursor = Cursor.DEFAULT;

    @Override
    public String clipboard() {
        return clipboard;
    }

    @Override
    public void setClipboard(String text) {
        clipboard = text == null ? "" : text;
    }

    @Override
    public boolean isKeyDown(int key) {
        return down.contains(key);
    }

    @Override
    public String keyName(int key, int scancode) {
        return Keys.fallbackName(key);
    }

    @Override
    public void setCursor(Cursor next) {
        cursor = next;
    }

    public Cursor cursor() {
        return cursor;
    }

    public void press(int key, boolean pressed) {
        if (pressed) down.add(key);
        else down.remove(key);
    }
}

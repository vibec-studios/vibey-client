package dev.henny.r3.mc.vui;

import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.input.InputCode;
import dev.henny.r3.mc.input.HeldInput;
import dev.henny.vui.render.Cursor;
import dev.henny.vui.render.Input;

final class McVuiInput implements Input {
    private final HeldInput held;
    private String clipboard = "";

    McVuiInput(HeldInput held) {
        this.held = held;
    }

    @Override
    public String clipboard() {
        try {
            String system = Game.mc().keyboardHandler.getClipboard();
            return system == null ? clipboard : system;
        } catch (RuntimeException e) {
            return clipboard;
        }
    }

    @Override
    public void setClipboard(String text) {
        clipboard = text == null ? "" : text;
        try {
            Game.mc().keyboardHandler.setClipboard(clipboard);
        } catch (RuntimeException ignored) {
            return;
        }
    }

    @Override
    public boolean isKeyDown(int key) {
        return held.isDown(InputCode.key(key));
    }

    @Override
    public String keyName(int key, int scancode) {
        return null;
    }

    @Override
    public void setCursor(Cursor cursor) {
    }
}

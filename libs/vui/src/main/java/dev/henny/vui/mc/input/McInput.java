package dev.henny.vui.mc.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.henny.vui.render.Cursor;
import dev.henny.vui.render.Input;
import net.minecraft.client.Minecraft;

public final class McInput implements Input {
    private Cursor cursor = Cursor.DEFAULT;

    @Override
    public String clipboard() {
        try {
            String s = Minecraft.getInstance().keyboardHandler.getClipboard();
            return s == null ? "" : s;
        } catch (RuntimeException e) {
            return "";
        }
    }

    @Override
    public void setClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text == null ? "" : text);
    }

    @Override
    public boolean isKeyDown(int key) {
        int code = KeyCodes.toNative(key);
        if (code < 0) return false;
        //? if >=26.3 {
        /*return InputConstants.isKeyDown(code);
        *///?} else if >=1.21.9 {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), code);
        //?} else
        /*return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), code);*/
    }

    @Override
    public String keyName(int key, int scancode) {
        return KeyNames.of(key);
    }

    @Override
    public void setCursor(Cursor next) {
        cursor = next == null ? Cursor.DEFAULT : next;
    }

    public Cursor cursor() {
        return cursor;
    }
}

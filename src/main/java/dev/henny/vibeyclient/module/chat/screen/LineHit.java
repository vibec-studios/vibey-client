package dev.henny.vibeyclient.module.chat.screen;

import net.minecraft.client.GuiMessage;

public record LineHit(GuiMessage.Line line, int top, int bottom, int right, boolean onButton) {
    public static final int BUTTON = 9;

    public int buttonX() {
        return right - BUTTON - 1;
    }

    public int buttonY() {
        return top + Math.max(0, (bottom - top - BUTTON) / 2);
    }
}

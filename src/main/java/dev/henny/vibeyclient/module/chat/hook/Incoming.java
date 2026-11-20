package dev.henny.vibeyclient.module.chat.hook;

import net.minecraft.client.GuiMessageTag;

public record Incoming(boolean drop, GuiMessageTag tag) {
    public static Incoming dropped() {
        return new Incoming(true, null);
    }

    public static Incoming keep(GuiMessageTag tag) {
        return new Incoming(false, tag);
    }
}

package dev.henny.vibeyclient.module.chat.window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.Mth;

public final class ChatSize {
    public static final int BOTTOM = 40;
    public static final int PAD = 12;

    private ChatSize() {}

    public static float scale() {
        return Minecraft.getInstance().options.chatScale().get().floatValue();
    }

    public static int lineHeight() {
        return (int) (9.0 * (Minecraft.getInstance().options.chatLineSpacing().get() + 1.0));
    }

    public static int localWidth() {
        return Mth.ceil(ChatComponent.getWidth(Minecraft.getInstance().options.chatWidth().get()) / Math.max(0.01f, scale()));
    }

    public static int width() {
        return Mth.ceil((localWidth() + PAD) * scale());
    }

    public static int height() {
        Options o = Minecraft.getInstance().options;
        int lines = ChatComponent.getHeight(o.chatHeightUnfocused().get()) / Math.max(1, lineHeight());
        return Math.max(lineHeight(), Mth.ceil(lines * lineHeight() * scale()));
    }

    public static int guiWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static int guiHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }
}

package dev.henny.vibeyclient.module.chat.window;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.chat.mixin.ChatComponentAccessor;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class ChatPreview {
    private static final int BG = 0x90000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int HINT = 0xFF8A8A9A;

    private ChatPreview() {}

    public static void draw(Gfx g, int w, int h, boolean preview) {
        g.rect(0, 0, w, h, BG);
        float f = ChatSize.scale();
        int p = ChatSize.lineHeight();
        List<GuiMessage.Line> lines = ((ChatComponentAccessor) Minecraft.getInstance().gui.getChat()).vibey$trimmedMessages();
        int fit = Math.max(1, (int) (h / (p * f)));
        if (lines.isEmpty()) {
            Icons.draw(g, Icons.CHAT, 4, h - 12, HINT);
            g.text(Lang.tr("chat.hud.empty"), 16, h - 11, HINT);
            return;
        }
        g.push();
        g.scale(f, f);
        for (int k = 0; k < Math.min(fit, lines.size()); k++) {
            int y = (int) (h / f) - (k + 1) * p + (p - 8) / 2;
            g.g.drawString(Minecraft.getInstance().font, lines.get(k).content(), 4, y, TEXT, true);
        }
        g.pop();
    }
}

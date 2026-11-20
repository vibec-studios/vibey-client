package dev.henny.vibeyclient.module.chat.screen;

import dev.henny.vibeyclient.module.chat.mixin.ChatComponentAccessor;
import dev.henny.vibeyclient.module.chat.window.ChatSize;
import dev.henny.vibeyclient.module.chat.window.ChatTransform;
import dev.henny.vibeyclient.module.chat.window.ChatWindow;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.Mth;

import java.util.List;

public final class ChatGeometry {
    private ChatGeometry() {}

    public static LineHit hit(double mouseX, double mouseY) {
        ChatComponent chat = Minecraft.getInstance().gui.getChat();
        ChatComponentAccessor acc = (ChatComponentAccessor) chat;
        List<GuiMessage.Line> lines = acc.vibey$trimmedMessages();
        if (lines.isEmpty()) return null;
        ChatTransform t = ChatWindow.transform();
        float f = ChatSize.scale();
        double x = t.localX(mouseX) / f - 4;
        double y = t.localY(mouseY) / f;
        int m = Mth.floor((ChatSize.guiHeight() - ChatSize.BOTTOM) / f);
        int p = ChatSize.lineHeight();
        int right = ChatSize.localWidth() + 8;
        if (x < -4 || x > right || y > m) return null;
        int k = (int) Math.floor((m - y) / p);
        int scroll = acc.vibey$scrollPos();
        int visible = Math.min(lines.size() - scroll, chat.getLinesPerPage());
        if (k < 0 || k >= visible) return null;
        int top = m - (k + 1) * p;
        int bottom = m - k * p;
        boolean onButton = x >= right - LineHit.BUTTON - 2 && x <= right;
        return new LineHit(lines.get(k + scroll), top, bottom, right, onButton);
    }

    public static void push(Gfx g) {
        ChatTransform t = ChatWindow.transform();
        float f = ChatSize.scale();
        g.push();
        if (!t.identity()) {
            g.translate(t.x, t.y);
            g.scale(t.scale, t.scale);
            g.translate(0, -t.anchorY);
        }
        g.scale(f, f);
        g.translate(4, 0);
    }
}

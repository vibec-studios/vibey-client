package dev.henny.vibeyclient.module.chat.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.privacy.Privacy;
import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.module.chat.hook.ChatHooks;
import dev.henny.vibeyclient.module.chat.line.Redact;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vui.mc.VuiMc;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;

public final class CopyButton {
    public static final String ICON = "chat_copy";
    private static final int ROW = 0x22FFFFFF;
    private static final int BUTTON_BG = 0xE0101018;
    private static final int ICON_IDLE = 0xFFB8B8C8;
    private static final int ICON_HOT = 0xFF3FE0C5;
    private static String lastCopied;

    private CopyButton() {}

    public static void register() {
        Icons.register(ICON, "..######.", "..#....#.", "######.#.", "#....#.#.", "#....#.#.", "#....###.", "#....#...",
                "######...", ".........");
    }

    public static String lastCopied() {
        return lastCopied;
    }

    public static boolean enabled() {
        ChatModule m = ChatModule.get();
        return m != null && m.isEnabled() && m.copyButton.on();
    }

    public static void draw(Gfx g, int mouseX, int mouseY) {
        if (!enabled()) return;
        LineHit h = ChatGeometry.hit(mouseX, mouseY);
        if (h == null) return;
        ChatGeometry.push(g);
        try {
            g.fill(-4, h.top(), h.right(), h.bottom(), ROW);
            g.fill(h.buttonX() - 1, h.buttonY() - 1, h.buttonX() + LineHit.BUTTON + 1, h.buttonY() + LineHit.BUTTON + 1, BUTTON_BG);
            Icons.draw(g, ICON, h.buttonX(), h.buttonY(), h.onButton() ? ICON_HOT : ICON_IDLE);
        } finally {
            g.pop();
        }
    }

    public static boolean click(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled()) return false;
        LineHit h = ChatGeometry.hit(mouseX, mouseY);
        if (h == null || !h.onButton()) return false;
        GuiMessage owner = ChatHooks.INDEX.owner(h.line());
        String text = owner != null ? ChatHooks.plain(owner) : lineText(h.line());
        copy(Redact.plain(text, Privacy.redactor()));
        return true;
    }

    public static void copy(String text) {
        lastCopied = text;
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        VuiMc.toasts().push(Lang.tr("chat.toast.copied"), "", "vibeyclient:check", "ok", 1.5f);
    }

    private static String lineText(GuiMessage.Line line) {
        StringBuilder sb = new StringBuilder();
        line.content().accept((i, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }
}

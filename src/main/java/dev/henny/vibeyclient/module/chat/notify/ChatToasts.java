package dev.henny.vibeyclient.module.chat.notify;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.privacy.Privacy;
import dev.henny.vibeyclient.module.chat.core.DirectMessage;
import dev.henny.vibeyclient.module.chat.line.Redact;
import dev.henny.vui.host.Toast;
import dev.henny.vui.mc.VuiMc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.ArrayList;
import java.util.List;

public final class ChatToasts {
    public static final float SECONDS = 6f;
    private static final int MAX_TEXT = 90;
    private static final int KEEP = 8;
    private static final List<Toast> SHOWN = new ArrayList<>();
    private static Toast last;

    private ChatToasts() {}

    public static Toast last() {
        return last;
    }

    public static void mention(String plain) {
        push(new Toast(Lang.tr("chat.toast.mention"), clip(Redact.plain(plain, Privacy.redactor())), "vibeyclient:chat", "info", SECONDS)
                .action(Lang.tr("chat.toast.open"), "open", () -> openChat("")));
    }

    public static void dm(DirectMessage dm) {
        boolean hide = Privacy.hideNames();
        String title = hide ? Privacy.aliases().alias(dm.sender()) : dm.sender();
        String reply = hide ? "/r " : "/msg " + dm.sender() + " ";
        push(new Toast(title, clip(Redact.plain(dm.text(), Privacy.redactor())), "vibeyclient:chat", "invite", SECONDS)
                .action(Lang.tr("chat.toast.reply"), "reply", () -> openChat(reply)));
    }

    private static void push(Toast t) {
        last = t;
        SHOWN.add(t);
        if (SHOWN.size() > KEEP) SHOWN.remove(0);
        VuiMc.toasts().stack().push(t);
    }

    public static void privacyChanged() {
        if (!Privacy.hideNames() || SHOWN.isEmpty()) return;
        for (Toast t : SHOWN) VuiMc.toasts().dismiss(t);
        SHOWN.clear();
    }

    private static void openChat(String text) {
        Minecraft.getInstance().setScreen(new ChatScreen(text, false));
    }

    private static String clip(String s) {
        return s.length() <= MAX_TEXT ? s : s.substring(0, MAX_TEXT - 1) + "…";
    }
}

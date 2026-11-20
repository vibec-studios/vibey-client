package dev.henny.vibeyclient.module.chat.window;

import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import org.joml.Matrix3x2f;

import java.util.function.Consumer;

public final class ChatWindow {
    public static final String ID = "chat.window";

    private static final HudLayout.Box BOX = new HudLayout.Box();
    private static final ChatTransform TRANSFORM = new ChatTransform();
    private static HudElement element;
    private static ChatModule module;

    private ChatWindow() {}

    public static void register(ChatModule m) {
        module = m;
        element = HudLayout.register(HudElement.builder(ID)
                .label("chat.hud.window")
                .description("chat.hud.window.desc")
                .icon(Icons.CHAT)
                .category(HudElement.Category.OTHER)
                .size(ChatSize::width, ChatSize::height)
                .defaults(Anchor.BOTTOM_LEFT, 0, -ChatSize.BOTTOM)
                .scale(1f, 0.5f, 2f)
                .render(ChatPreview::draw)
                .visibleWhen(ChatWindow::active)
                .build());
    }

    public static void shown(boolean on) {
        if (on && element != null && HudLayout.hidden(element)) HudLayout.setHidden(element, false);
    }

    public static HudElement element() {
        return element;
    }

    public static boolean active() {
        return element != null && module != null && module.isEnabled() && module.movable.on();
    }

    public static boolean hideUnfocused() {
        return active() && (HudLayout.editing() || HudLayout.hidden(element));
    }

    public static ChatTransform transform() {
        ChatTransform t = TRANSFORM;
        int sh = ChatSize.guiHeight();
        t.anchorY = sh - ChatSize.BOTTOM;
        if (!active()) {
            t.x = 0;
            t.y = t.anchorY;
            t.scale = 1f;
            return t;
        }
        HudLayout.box(element, ChatSize.guiWidth(), sh, BOX);
        t.x = BOX.x;
        t.y = BOX.y + BOX.h;
        t.scale = BOX.scale;
        return t;
    }

    public static Consumer<Matrix3x2f> wrap(Consumer<Matrix3x2f> vanilla) {
        ChatTransform t = transform();
        if (t.identity()) return vanilla;
        float x = t.x, y = t.y, s = t.scale, a = t.anchorY;
        return m -> {
            m.translate(x, y);
            m.scale(s, s);
            m.translate(0, -a);
            vanilla.accept(m);
        };
    }
}

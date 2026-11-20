package dev.henny.vibeyclient.module.chat.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.module.chat.core.SearchQuery;
import dev.henny.vibeyclient.module.chat.hook.ChatHooks;
import dev.henny.vibeyclient.module.chat.hook.ChatRefresh;
import dev.henny.vibeyclient.module.chat.mixin.ChatComponentAccessor;
import dev.henny.vui.dom.Document;
import dev.henny.vui.mc.VuiMc;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

public final class SearchMode {
    public static final String DOCUMENT = "vibeyclient:chat/search";

    private static Document overlay;
    private static SearchController controller;
    private static EditBox input;
    private static String draft = "";
    private static boolean enterOnInit;

    private SearchMode() {}

    public static boolean active() {
        return SearchState.open();
    }

    public static SearchController controller() {
        return controller;
    }

    public static void openChat() {
        Minecraft mc = Minecraft.getInstance();
        if (!ChatModule.on() || mc.level == null) return;
        enterOnInit = true;
        mc.setScreen(new ChatScreen("", false));
    }

    public static void initialized(EditBox box) {
        if (!enterOnInit) return;
        enterOnInit = false;
        enter(box);
    }

    public static void toggle(EditBox box) {
        if (active()) exit();
        else enter(box);
    }

    public static void enter(EditBox box) {
        if (!ChatModule.on() || active()) return;
        input = box;
        draft = box.getValue();
        box.setValue("");
        box.setHint(Lang.text("chat.search.hint"));
        SearchState.open(true);
        controller = new SearchController();
        overlay = VuiMc.overlays().show(DOCUMENT, controller);
        controller.update("", 0);
    }

    public static void edited(String text) {
        SearchState.query(text);
        ChatRefresh.now();
        if (controller != null) controller.update(text, count());
    }

    public static void frame() {
        if (active() && overlay != null && !VuiMc.overlays().documents().contains(overlay)) exit();
    }

    public static void exit() {
        if (!active()) return;
        SearchState.open(false);
        if (overlay != null) VuiMc.overlays().close(overlay);
        overlay = null;
        controller = null;
        EditBox box = input;
        input = null;
        if (box != null) {
            box.setHint(Component.empty());
            box.setValue(draft);
        }
        ChatRefresh.now();
    }

    public static int count() {
        SearchQuery q = SearchState.query();
        if (!q.active()) return 0;
        int n = 0;
        for (GuiMessage g : ((ChatComponentAccessor) Minecraft.getInstance().gui.getChat()).vibey$allMessages()) {
            if (q.matches(ChatHooks.plain(g))) n++;
        }
        return n;
    }
}

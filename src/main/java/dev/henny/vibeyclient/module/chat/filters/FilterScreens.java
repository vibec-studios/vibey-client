package dev.henny.vibeyclient.module.chat.filters;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vui.mc.host.VuiScreen;

public final class FilterScreens {
    public static final String DOCUMENT = "vibeyclient:chat/filters";

    private FilterScreens() {}

    public static VuiScreen open() {
        ChatModule m = ChatModule.get();
        if (m == null) return null;
        VuiScreen s = new VuiScreen(DOCUMENT, new FilterController(m));
        s.parent(ClientCompat.screen()).pause(false);
        s.open();
        return s;
    }
}

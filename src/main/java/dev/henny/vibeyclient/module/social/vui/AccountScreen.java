package dev.henny.vibeyclient.module.social.vui;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.ui.vui.SyncedScreen;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import net.minecraft.client.gui.screens.Screen;

public final class AccountScreen extends SyncedScreen {
    public static final String DOC = "vibeyclient:social/account";

    private final AccountController controller;

    private AccountScreen(Screen parent, SocialModule module, Runnable back) {
        this(parent, new AccountController(module, back));
    }

    private AccountScreen(Screen parent, AccountController controller) {
        super(DOC, controller, parent);
        this.controller = controller;
        background(!ClientCompat.inWorld());
    }

    public static void open(Screen parent, SocialModule module) {
        VibeyVui.init();
        ClientCompat.openScreen(new AccountScreen(parent, module, () -> ClientCompat.openScreen(parent)));
    }

    @Override
    protected void sync() {
        controller.sync();
    }
}

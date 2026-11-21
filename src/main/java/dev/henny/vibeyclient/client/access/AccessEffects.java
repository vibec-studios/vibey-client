package dev.henny.vibeyclient.client.access;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.access.AccessView;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.vui.Menus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

final class AccessEffects {
    private AccessEffects() {}

    static void changed(AccessView before, AccessView now) {
        boolean wasOpen = before != null && before.open();
        if (wasOpen == now.open()) return;
        Vibey.LOG.info("Vibe access {}: {}", now.open() ? "granted" : "closed", now.phase());
        if (!now.open()) {
            ModuleManager.get().suspend();
            if (Menus.isOpen()) ClientCompat.openScreen(Minecraft.getInstance().level == null ? new TitleScreen() : null);
        }
        if (before != null && ClientCompat.screen() instanceof TitleScreen) ClientCompat.openScreen(new TitleScreen());
    }
}

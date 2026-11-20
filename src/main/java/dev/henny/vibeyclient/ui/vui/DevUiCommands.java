package dev.henny.vibeyclient.ui.vui;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.dev.DevDriver;
import dev.henny.vibeyclient.ui.vui.onboarding.Onboarding;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;

public final class DevUiCommands {
    private DevUiCommands() {}

    public static void register() {
        DevDriver.EXTRA.put("menu", arg -> {
            String[] a = arg.split(" ");
            String module = a.length > 0 && !a[0].isEmpty() && !a[0].equals("-") ? a[0] : null;
            String panel = a.length > 1 ? a[1] : "";
            ClientCompat.openScreen(null);
            Menus.openAt(ClientCompat.inWorld() ? null : new TitleScreen(), module, panel);
            return "ok";
        });
        DevDriver.EXTRA.put("title", arg -> {
            ClientCompat.openScreen(new TitleScreen());
            return "ok";
        });
        DevDriver.EXTRA.put("pause", arg -> {
            ClientCompat.openScreen(new PauseScreen(true));
            return "ok";
        });
        DevDriver.EXTRA.put("onboarding", arg -> {
            ClientCompat.openScreen(Onboarding.create(new TitleScreen()));
            return "ok";
        });
        DevDriver.EXTRA.put("lang", arg -> {
            GlobalSettings.language.set(arg.startsWith("de") ? GlobalSettings.Language.GERMAN : GlobalSettings.Language.ENGLISH);
            Lang.tick();
            return "ok " + Lang.code();
        });
        DevDriver.EXTRA.put("uiscale", arg -> {
            GlobalSettings.uiScale.set(GlobalSettings.UiScale.valueOf(arg.isEmpty() ? "GAME" : arg));
            return "ok";
        });
    }
}

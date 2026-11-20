package dev.henny.vibeyclient.ui.vui;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import dev.henny.vibeyclient.ui.vui.menu.MenuScreen;
import net.minecraft.client.gui.screens.Screen;

public final class Menus {
    private Menus() {}

    public static boolean isOpen() {
        Screen s = ClientCompat.screen();
        return s instanceof VibeyScreen || s instanceof MenuScreen;
    }

    public static void open(Screen parent) {
        openAt(parent, null, "");
    }

    public static void openModule(Screen parent, String moduleId) {
        openAt(parent, moduleId, "");
    }

    public static void openFriends(Screen parent) {
        openAt(parent, null, "friends");
    }

    public static void openQuick(Screen parent) {
        openAt(parent, null, "quick");
    }

    public static void openAbout(Screen parent) {
        openAt(parent, null, "about");
    }

    public static void openAt(Screen parent, String moduleId, String panel) {
        if (ClassicUi.enabled()) {
            openClassic(parent, moduleId, panel);
            return;
        }
        MenuScreen.open(parent, moduleId, panel);
    }

    private static void openClassic(Screen parent, String moduleId, String panel) {
        String tab = switch (panel) {
            case "friends" -> VibeyTabs.exists("account") ? "account" : VibeyTabs.MODULES;
            case "quick" -> VibeyTabs.GENERAL;
            case "about" -> VibeyTabs.ABOUT;
            default -> VibeyTabs.MODULES;
        };
        if (tab.equals(VibeyTabs.MODULES)) ModulesPanel.preselect(moduleId);
        ClientCompat.openScreen(new VibeyScreen(parent, tab));
    }
}

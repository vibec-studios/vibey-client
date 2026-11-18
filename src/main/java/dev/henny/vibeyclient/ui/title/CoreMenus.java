package dev.henny.vibeyclient.ui.title;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.SocialStatus;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.MenuEntries.Entry;
import dev.henny.vibeyclient.ui.MenuEntries.Place;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.minecraft.client.gui.screens.Screen;

/**
 * The entries the core puts on the title screen (small icon buttons in a corner, {@link TitleButtons}) and into the
 * pause menu row. "wardrobe" and "friends" are defaults that open the matching Vibey menu tab (or the module's page
 * while that tab does not exist); the owning modules register their own entry with the same id to replace them.
 */
public final class CoreMenus {
    private CoreMenus() {}

    /** Tab ids the default entries look for (first one registered wins). */
    private static final String[] WARDROBE_TABS = {"wardrobe", "cosmetics"};
    private static final String[] FRIENDS_TABS = {"friends", "social"};

    public static void register() {
        // ---------------------------------------------------------------- title (small icon buttons in a corner)
        MenuEntries.register(Place.TITLE, new Entry("wardrobe", "core.title.wardrobe", Icons.SHIRT, 10,
                MenuEntries.whenEnabled("cosmetics"), p -> openTab(p, WARDROBE_TABS, "cosmetics")));
        MenuEntries.register(Place.TITLE, new Entry("friends", "core.title.friends", Icons.FRIENDS, 20,
                MenuEntries.whenOnline("social"), p -> openTab(p, FRIENDS_TABS, "social"), CoreMenus::socialBadge));
        // 30: "media" (the media module registers it)
        MenuEntries.register(Place.TITLE, new Entry("vibey", "core.title.vibey", Icons.SEAL, 100,
                () -> true, p -> ClientCompat.openScreen(new VibeyScreen(p))));

        // ---------------------------------------------------------------- pause
        MenuEntries.register(Place.PAUSE, new Entry("vibey", "core.pause.vibey", Icons.SEAL, 0,
                () -> true, p -> ClientCompat.openScreen(new VibeyScreen(p))));
        MenuEntries.register(Place.PAUSE, new Entry("wardrobe", "core.title.wardrobe", Icons.SHIRT, 10,
                MenuEntries.whenEnabled("cosmetics"), p -> openTab(p, WARDROBE_TABS, "cosmetics")));
        MenuEntries.register(Place.PAUSE, new Entry("friends", "core.title.friends", Icons.FRIENDS, 20,
                MenuEntries.whenOnline("social"), p -> openTab(p, FRIENDS_TABS, "social"), CoreMenus::socialBadge));
        // No "host" pause entry: the multiplayer module puts "Host World" in place of vanilla's "Open to LAN".
    }

    private static int socialBadge() {
        return Services.get(SocialStatus.class).map(SocialStatus::badge).orElse(0);
    }

    /** Opens the first existing Vibey tab of {@code tabs}, else the settings page of {@code moduleId}. */
    public static void openTab(Screen parent, String[] tabs, String moduleId) {
        for (String t : tabs) {
            for (VibeyTabs.Tab tab : VibeyTabs.visible()) {
                if (tab.id().equals(t)) {
                    ClientCompat.openScreen(new VibeyScreen(parent, t));
                    return;
                }
            }
        }
        ModulesPanel.preselect(moduleId);
        ClientCompat.openScreen(new VibeyScreen(parent, VibeyTabs.MODULES));
    }

    /** Whether the first-start welcome should be shown before the title screen. */
    public static boolean needsOnboarding() {
        return !GlobalSettings.onboarded();
    }
}

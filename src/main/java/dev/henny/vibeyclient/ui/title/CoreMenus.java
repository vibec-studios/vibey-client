package dev.henny.vibeyclient.ui.title;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.api.service.SocialStatus;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.MenuEntries.Entry;
import dev.henny.vibeyclient.ui.MenuEntries.Place;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

/**
 * The entries the core puts on the Vibey title screen and into the pause menu row. "wardrobe", "friends" and "host"
 * are defaults that open the matching Vibey menu tab (or the module's settings page while that tab does not exist);
 * the owning modules may register their own entry with the same id to replace them.
 */
public final class CoreMenus {
    private CoreMenus() {}

    /** Tab ids the default entries look for (first one registered wins). */
    private static final String[] WARDROBE_TABS = {"wardrobe", "cosmetics"};
    private static final String[] FRIENDS_TABS = {"friends", "social"};

    public static void register() {
        // ---------------------------------------------------------------- title
        MenuEntries.register(Place.TITLE, new Entry("singleplayer", "core.title.singleplayer", Icons.PERSON, 0,
                () -> true, p -> ClientCompat.openScreen(new SelectWorldScreen(p))));
        MenuEntries.register(Place.TITLE, new Entry("multiplayer", "core.title.multiplayer", Icons.SERVER, 10,
                () -> true, CoreMenus::multiplayer));
        MenuEntries.register(Place.TITLE, new Entry("wardrobe", "core.title.wardrobe", Icons.SHIRT, 20,
                MenuEntries.whenEnabled("cosmetics"), p -> openTab(p, WARDROBE_TABS, "cosmetics")));
        MenuEntries.register(Place.TITLE, new Entry("friends", "core.title.friends", Icons.FRIENDS, 30,
                MenuEntries.whenOnline("social"), p -> openTab(p, FRIENDS_TABS, "social"), CoreMenus::socialBadge));
        MenuEntries.register(Place.TITLE, new Entry("vibey", "core.title.vibey", Icons.SEAL, 40,
                () -> true, p -> ClientCompat.openScreen(new VibeyScreen(p))));
        MenuEntries.register(Place.TITLE, new Entry("options", "core.title.options", Icons.GEAR, 50,
                () -> true, p -> ClientCompat.openScreen(new OptionsScreen(p, Minecraft.getInstance().options))));
        MenuEntries.register(Place.TITLE, new Entry("mods", "core.title.mods", Icons.PUZZLE, 60,
                () -> modsScreenClass() != null, CoreMenus::openMods));
        MenuEntries.register(Place.TITLE, new Entry("quit", "core.title.quit", Icons.POWER, 1000,
                () -> true, p -> Minecraft.getInstance().stop()));

        // ---------------------------------------------------------------- pause
        MenuEntries.register(Place.PAUSE, new Entry("vibey", "core.pause.vibey", Icons.SEAL, 0,
                () -> true, p -> ClientCompat.openScreen(new VibeyScreen(p))));
        MenuEntries.register(Place.PAUSE, new Entry("wardrobe", "core.title.wardrobe", Icons.SHIRT, 10,
                MenuEntries.whenEnabled("cosmetics"), p -> openTab(p, WARDROBE_TABS, "cosmetics")));
        MenuEntries.register(Place.PAUSE, new Entry("friends", "core.title.friends", Icons.FRIENDS, 20,
                MenuEntries.whenOnline("social"), p -> openTab(p, FRIENDS_TABS, "social"), CoreMenus::socialBadge));
        MenuEntries.register(Place.PAUSE, new Entry("host", "core.pause.host", Icons.HOST, 30,
                () -> ModuleManager.get().isEnabled("multiplayer") && Services.get(HostingService.class).map(HostingService::canHost).orElse(false),
                p -> Services.get(HostingService.class).ifPresent(HostingService::openHostScreen)));
    }

    private static int socialBadge() {
        return Services.get(SocialStatus.class).map(SocialStatus::badge).orElse(0);
    }

    /** Vanilla's multiplayer button: the safety notice first (unless skipped). */
    static void multiplayer(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        ClientCompat.openScreen(mc.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(parent) : new SafetyScreen(parent));
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

    // ---------------------------------------------------------------- mod list (Mod Menu on Fabric, NeoForge's own)

    private static Class<?> modsClass;
    private static boolean modsChecked;

    private static Class<?> modsScreenClass() {
        if (!modsChecked) {
            modsChecked = true;
            String name = null;
            if (Vibey.platform().isModLoaded("modmenu")) name = "com.terraformersmc.modmenu.gui.ModsScreen";
            else if (!Vibey.platform().isFabric()) name = "net.neoforged.neoforge.client.gui.ModListScreen";
            if (name != null) {
                try {
                    modsClass = Class.forName(name);
                } catch (Throwable t) {
                    Vibey.LOG.debug("No mod list screen: {}", t.toString());
                }
            }
        }
        return modsClass;
    }

    private static void openMods(Screen parent) {
        try {
            Object s = modsScreenClass().getConstructor(Screen.class).newInstance(parent);
            ClientCompat.openScreen((Screen) s);
        } catch (Throwable t) {
            Vibey.LOG.warn("Could not open the mod list: {}", t.toString());
        }
    }

    /** Whether the first-start welcome should be shown before the title screen. */
    public static boolean needsOnboarding() {
        return !GlobalSettings.onboarded();
    }
}

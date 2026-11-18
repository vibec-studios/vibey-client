package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.HudIcons;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudShare;
import dev.henny.vibeyclient.ui.hud.HudTemplates;
import net.minecraft.client.Minecraft;

/**
 * HUD: the built-in widgets (FPS, CPS, keystrokes, coordinates, armour, potion effects, clock, ping, server, speed,
 * in-game time, item counters, memory, compass), the templates ("PvP", "Minimal", "Building", "Streamer") and the
 * ways into the HUD editor (key, pause menu entry, this module's page). The editor and the widget API live in the
 * core ({@code ui/hud}, docs/HUD.md); other modules add their own widgets there. Fabric 1.21.11 only for now.
 */
public final class HudModule extends VibeyModule {
    public static final String ID = "hud";
    /** The template a fresh install starts with. */
    public static final String DEFAULT_TEMPLATE = "minimal";
    private static HudModule instance;

    public final ActionSetting edit = action("edit", () -> HudLayout.openEditor(ClientCompat.screen()));
    public final KeybindSetting editor = keybind("editor", Keys.H).onPress(() -> {
        if (ClientCompat.screen() == null) HudLayout.openEditor(null);
    });
    public final BoolSetting hideF3 = bool("hideF3", true);
    public final ActionSetting resetLayout = action("resetLayout", this::resetToDefault).advanced();
    public final ActionSetting intro = action("intro", () -> {
        HudLayout.setFlag("intro", false);
        HudLayout.openEditor(ClientCompat.screen());
    }).advanced();
    public final ActionSetting folder = action("folder", () -> {
        try {
            java.nio.file.Files.createDirectories(HudShare.folder());
            ClientCompat.openFolder(HudShare.folder());
        } catch (Exception e) {
            log.warn("Could not open {}", HudShare.folder(), e);
        }
    }).advanced();

    private boolean defaultsApplied;

    public HudModule() {
        super(ID, ModuleCategory.MISC);
        instance = this;
        HudIcons.init();
    }

    public static HudModule instance() {
        return instance;
    }

    @Override
    public int accentColor() {
        return Theme.BRAND2;
    }

    @Override
    public void onLoad() {
        BuiltinWidgets.registerAll(this);
        BuiltinTemplates.registerAll();
        MenuEntries.register(MenuEntries.Place.PAUSE, new MenuEntries.Entry("hud", "hud.menu", HudIcons.LIBRARY, 25,
                MenuEntries.whenEnabled(ID), HudLayout::openEditor));
        HudLayout.hideWidgetsWhen(() -> hideF3.get() && debugOpen());
    }

    @Override
    public void onEnable() {
        // first start: a nice layout instead of an empty screen
        if (!defaultsApplied && HudLayout.fresh()) {
            defaultsApplied = true;
            if (HudTemplates.apply(DEFAULT_TEMPLATE)) HudLayout.save();
        }
    }

    @Override
    public void tick() {
        Motion.tick();
    }

    @Override
    public void renderHud(Gfx g) {
        // clicks are sampled every frame (a tick is too slow for 15+ CPS)
        Clicks.sample();
    }

    @Override
    public void onWorldLeave() {
        Motion.reset();
    }

    private void resetToDefault() {
        HudLayout.resetAll();
        HudTemplates.apply(DEFAULT_TEMPLATE);
        HudLayout.save();
    }

    private static boolean debugOpen() {
        try {
            return Minecraft.getInstance().getDebugOverlay().showDebugScreen();
        } catch (RuntimeException e) {
            return false;
        }
    }
}

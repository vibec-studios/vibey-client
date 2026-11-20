package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.hud.timer.Timers;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.HudIcons;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudShare;
import dev.henny.vibeyclient.ui.hud.HudTemplates;
import net.minecraft.client.Minecraft;

public final class HudModule extends VibeyModule {
    public static final String ID = "hud";
    public static final String DEFAULT_TEMPLATE = "minimal";
    private static HudModule instance;

    public final ActionSetting edit = action("edit", () -> HudLayout.openEditor(ClientCompat.screen()));
    public final KeybindSetting editor = keybind("editor", Keys.H).onPress(() -> {
        if (ClientCompat.screen() == null) HudLayout.openEditor(null);
    });
    public final KeybindSetting timerKey = keybind("timerKey", Keys.U).onPress(Timers::toggle);
    public final KeybindSetting timerLap = keybind("timerLap", Keys.N).onPress(Timers::lap);
    public final BoolSetting hideF3 = bool("hideF3", true);
    public final BoolSetting hideVanillaEffects = bool("hideVanillaEffects", true);
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
    public java.util.List<String> serverRuleKeys() {
        return BuiltinWidgets.RULE_KEYS;
    }

    @Override
    public int accentColor() {
        return Theme.BRAND2;
    }

    @Override
    public void onLoad() {
        BuiltinWidgets.registerAll(this);
        ExtraWidgets.registerAll(this);
        BuiltinTemplates.registerAll();
        HudTemplates.setDefault(DEFAULT_TEMPLATE);
        MenuEntries.register(MenuEntries.Place.PAUSE, new MenuEntries.Entry("hud", "hud.menu", HudIcons.LIBRARY, 25,
                MenuEntries.whenEnabled(ID), HudLayout::openEditor));
        HudLayout.hideWidgetsWhen(() -> hideF3.get() && debugOpen());
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.replaceElement(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.STATUS_EFFECTS,
                vanilla -> (g, tick) -> {
                    if (!effectsReplaceVanilla()) vanilla.render(g, tick);
                });
    }

    private boolean effectsReplaceVanilla() {
        if (!isEnabled() || !hideVanillaEffects.get()) return false;
        dev.henny.vibeyclient.ui.hud.HudElement e = HudLayout.get("hud.effects");
        return e != null && !HudLayout.hidden(e) && !HudLayout.serverDisabled(e);
    }

    @Override
    public void onEnable() {
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
        Clicks.sample();
    }

    @Override
    public void onWorldLeave() {
        Motion.reset();
    }

    public boolean defaultsApplied() {
        return defaultsApplied;
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

package dev.henny.vibeyclient.module.screens;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.screens.edit.EditMode;
import dev.henny.vibeyclient.module.screens.hook.ScreenHooks;
import dev.henny.vibeyclient.module.screens.store.LayoutFile;

public final class ScreensModule extends VibeyModule {
    private static volatile ScreensModule instance;

    public final ActionSetting tryIt = action("tryIt", ScreenPages::tryOnOptions);
    public final BoolSetting pencil = bool("pencil", true);
    public final KeybindSetting editKey = keybind("editKey", Keys.F9);
    public final ActionSetting resetScreen;
    public final ActionSetting resetAll;
    public final StringSetting blocked = string("blocked", "", 400).advanced();

    public ScreensModule() {
        super("screens", ModuleCategory.MISC);
        instance = this;
        settings().add(ScreenPages.picker());
        resetScreen = action("resetScreen", ScreenPages::resetPicked).section("edited").visibleWhen(ScreenPages::any);
        resetAll = action("resetAll", ScreenPages::resetAll).section("edited").visibleWhen(ScreenPages::any);
    }

    public static ScreensModule get() {
        return instance;
    }

    @Override
    public int accentColor() {
        return 0xFFFFB547;
    }

    @Override
    public void onLoad() {
        Screens.attach(this);
        Screens.blocklist(blocked.get());
        blocked.onChange(Screens::blocklist);
        LayoutFile.load(Screens.store());
        Screens.store().onChange(() -> LayoutFile.saveSoon(Screens.store()));
        ScreenHooks.register();
    }

    @Override
    public void onEnable() {
        ScreenPages.refreshCurrent();
    }

    @Override
    public void onDisable() {
        EditMode.leave();
        ScreenPages.refreshCurrent();
    }
}

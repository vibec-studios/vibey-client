package dev.henny.vibeyclient.module.skins;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.skins.ui.SkinLibraryScreen;

import java.util.List;

public final class SkinsModule extends VibeyModule {
    private static volatile SkinsModule instance;
    final SkinSwap swap = new SkinSwap();

    public final ActionSetting openLibrary = action("openLibrary", () -> SkinLibraryScreen.open(ClientCompat.screen()));
    public final KeybindSetting libraryKey = keybind("libraryKey", Keys.UNBOUND).onPress(() -> SkinLibraryScreen.open(null));

    public SkinsModule() {
        super("skins", ModuleCategory.MISC);
        instance = this;
    }

    public static SkinsModule get() {
        return instance;
    }

    public SkinSwap swap() {
        return swap;
    }

    @Override
    public int accentColor() {
        return 0xFF4AB8FF;
    }

    @Override
    public List<String> notices() {
        swap.refreshIfStale(5000);
        return switch (swap.availability()) {
            case NO_LAUNCHER -> List.of(tr("notice.noLauncher"));
            case NO_LIST -> List.of(tr("notice.noList"));
            default -> List.of();
        };
    }

    @Override
    public void onLoad() {
        swap.startup();
        swap.refresh();
    }

    @Override
    public void onDisable() {
        swap.reset();
        LocalSkin.clear();
    }

    @Override
    public void tick() {
        swap.refreshIfStale(10_000);
        swap.tick();
    }
}

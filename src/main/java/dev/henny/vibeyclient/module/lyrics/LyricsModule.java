package dev.henny.vibeyclient.module.lyrics;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Synced lyrics in the HUD, sky and world (port of Vibey Lyrics, Fabric only). Conflicts with "vibeylyrics".
 * Stub from the core skeleton: the module owner fills it in. Mixins go to {@code module/lyrics/mixin} and are listed in
 * {@code vibeyclient.lyrics.mixins.json}.
 */
public final class LyricsModule extends VibeyModule {

    public final KeybindSetting open = keybind("open", GLFW.GLFW_KEY_K);
    public final BoolSetting hideInMenus = bool("hideInMenus", true);

    /** Fabric only, like the standalone mod (this package is not even compiled for NeoForge). */
    @Override
    public boolean available() {
        return Vibey.platform().isFabric();
    }

    public LyricsModule() {
        super("lyrics", ModuleCategory.MEDIA);
    }

    @Override
    public int accentColor() {
        return 0xFFFF5470;
    }
}

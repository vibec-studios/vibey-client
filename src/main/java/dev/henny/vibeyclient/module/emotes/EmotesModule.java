package dev.henny.vibeyclient.module.emotes;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Emote wheel and emotes with props, particles and sounds.
 * Stub from the core skeleton: the module owner fills it in. Mixins go to {@code module/emotes/mixin} and are listed in
 * {@code vibeyclient.emotes.mixins.json}.
 */
public final class EmotesModule extends VibeyModule {

    public final KeybindSetting wheel = keybind("wheel", GLFW.GLFW_KEY_B);
    public final BoolSetting thirdPerson = bool("thirdPerson", true);
    public final FloatSetting volume = floatRange("volume", 1f, 0f, 1f, 0.05f);
    public final BoolSetting showOthers = bool("showOthers", true);

    public EmotesModule() {
        super("emotes", ModuleCategory.COSMETICS);
    }

    @Override
    public int accentColor() {
        return 0xFFFFB547;
    }
}

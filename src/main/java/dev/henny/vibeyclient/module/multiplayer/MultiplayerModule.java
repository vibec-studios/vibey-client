package dev.henny.vibeyclient.module.multiplayer;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;

/**
 * Host your singleplayer world for friends (port of Vibey Multiplayer). Conflicts with the standalone mod "vibey".
 * Stub from the core skeleton: the module owner fills it in. Mixins go to {@code module/multiplayer/mixin} and are listed in
 * {@code vibeyclient.multiplayer.mixins.json}.
 */
public final class MultiplayerModule extends VibeyModule {

    public final StringSetting relay = string("relay", BuildInfo.DEFAULT_RELAY, 128).advanced();

    public MultiplayerModule() {
        super("multiplayer", ModuleCategory.MULTIPLAYER);
    }

    @Override
    public int accentColor() {
        return 0xFF3FE0C5;
    }
}

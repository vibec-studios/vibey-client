package dev.henny.vibeyclient.module.devlink;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;

/**
 * Blockbench live link (SPEC §8): a WebSocket on 127.0.0.1 for pushing .vibey bundles into the game.
 * Stub from the core skeleton: the module owner fills it in. Mixins go to {@code module/devlink/mixin} and are listed in
 * {@code vibeyclient.devlink.mixins.json}.
 */
public final class DevLinkModule extends VibeyModule {

    public final BoolSetting creatorMode = bool("creatorMode", Vibey.platform().isDevelopment());
    public final IntSetting port = intRange("port", 47821, 1024, 65535).advanced();

    public DevLinkModule() {
        super("devlink", ModuleCategory.CREATOR);
    }

    @Override
    public int accentColor() {
        return 0xFF6B6B75;
    }
}

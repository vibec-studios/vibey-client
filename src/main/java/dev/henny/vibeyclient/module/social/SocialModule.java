package dev.henny.vibeyclient.module.social;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;

/**
 * Friends, requests, invites, chats and presence on top of the Vibe backend.
 * Stub from the core skeleton: the module owner fills it in. Mixins go to {@code module/social/mixin} and are listed in
 * {@code vibeyclient.social.mixins.json}.
 */
public final class SocialModule extends VibeyModule {

    public final BoolSetting showPresence = bool("showPresence", true);
    public final BoolSetting friendRequests = bool("friendRequests", true);
    public final BoolSetting toasts = bool("toasts", true);

    /** Social needs the backend: hidden while online features are off. */
    @Override
    public boolean visible() {
        return GlobalSettings.online();
    }

    public SocialModule() {
        super("social", ModuleCategory.SOCIAL);
    }

    @Override
    public int accentColor() {
        return 0xFF3FE0C5;
    }
}

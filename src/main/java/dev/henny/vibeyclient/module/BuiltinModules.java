package dev.henny.vibeyclient.module;

import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.module.cosmetics.CosmeticsModule;
import dev.henny.vibeyclient.module.devlink.DevLinkModule;
import dev.henny.vibeyclient.module.emotes.EmotesModule;
import dev.henny.vibeyclient.module.multiplayer.MultiplayerModule;
import dev.henny.vibeyclient.module.social.SocialModule;

/** The modules that ship with the client, in menu order. Add new modules here (one line). */
public final class BuiltinModules {
    private BuiltinModules() {}

    public static void registerAll(ModuleManager mm) {
        mm.register(new SocialModule());
        mm.register(new CosmeticsModule());
        mm.register(new EmotesModule());
        mm.register(new MultiplayerModule());
        //? if fabric
        mm.register(new dev.henny.vibeyclient.module.lyrics.LyricsModule());
        mm.register(new DevLinkModule());
    }
}

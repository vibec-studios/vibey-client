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
        // Video screens: only in the Fabric 1.21.11 jar for now (gradle/common.gradle excludes the package elsewhere).
        // TEMPORARY shim until the Media module hosts the feature (docs/VIDEO.md, "Wiring").
        registerIfPresent(mm, "dev.henny.vibeyclient.module.media.video.VideoModule");
        mm.register(new dev.henny.vibeyclient.module.nametags.NametagsModule());
        mm.register(new DevLinkModule());
    }

    /** Registers a module whose package is only part of some builds. */
    private static void registerIfPresent(ModuleManager mm, String className) {
        try {
            Class<?> c = Class.forName(className);
            mm.register((dev.henny.vibeyclient.api.VibeyModule) c.getDeclaredConstructor().newInstance());
        } catch (ClassNotFoundException absent) {
            // not in this jar
        } catch (ReflectiveOperationException | LinkageError e) {
            dev.henny.vibeyclient.Vibey.LOG.error("Could not create module {}", className, e);
        }
    }
}

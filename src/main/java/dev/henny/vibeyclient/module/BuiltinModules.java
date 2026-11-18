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
        mm.register(new dev.henny.vibeyclient.module.media.MediaModule());
        mm.register(new dev.henny.vibeyclient.module.nametags.NametagsModule());
        // PvP basics: only in the Fabric 1.21.11 jar for now (gradle/common.gradle leaves the package out elsewhere).
        registerIfPresent(mm, "dev.henny.vibeyclient.module.pvp.PvpModule");
        mm.register(new DevLinkModule());
    }

    /** Registers a module whose package is only part of some builds. */
    private static void registerIfPresent(ModuleManager mm, String className) {
        try {
            mm.register((dev.henny.vibeyclient.api.VibeyModule) Class.forName(className).getConstructor().newInstance());
        } catch (ClassNotFoundException absent) {
            // not in this jar
        } catch (ReflectiveOperationException | LinkageError e) {
            dev.henny.vibeyclient.Vibey.LOG.error("Could not create module {}", className, e);
        }
    }
}

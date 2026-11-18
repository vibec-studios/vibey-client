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
        mm.register(new DevLinkModule());
        // HUD widgets + templates: Fabric 1.21.11 only for now (gradle/common.gradle leaves the package out elsewhere)
        registerIfPresent(mm, "dev.henny.vibeyclient.module.hud.HudModule");
    }

    private static void registerIfPresent(ModuleManager mm, String className) {
        try {
            Class<?> c = Class.forName(className);
            mm.register((dev.henny.vibeyclient.api.VibeyModule) c.getConstructor().newInstance());
        } catch (ClassNotFoundException e) {
            // not built for this target
        } catch (ReflectiveOperationException | LinkageError e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Could not create {}", className, e);
        }
    }
}

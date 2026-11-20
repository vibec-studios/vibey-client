package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.cinematic.CinematicModule;
import dev.henny.vibeyclient.module.cinematic.CinematicRules;
import dev.henny.vibeyclient.module.cinematic.core.plan.Mode;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.library.SongLibrary;
import dev.henny.vibeyclient.ui.vui.Menus;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.Locale;

import static dev.henny.vibeyclient.gametest.ReviewKit.check;

public final class CinematicGameTest implements FabricClientGameTest {
    static final ReviewKit KIT = new ReviewKit("cinematic");

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            CinematicModule m = CinematicModule.get();
            check(m != null && m.isEnabled(), "cinematic module should be on");
            check(ServerRules.supportedKeys().contains(CinematicRules.AFK), "rule key not declared");
            m.settings().all().forEach(s -> s.reset());
            m.auto.set(false);
            m.grain.set(true);
            KIT.log("start key conflicts: '%s'", dev.henny.vibeyclient.client.MappingKeys.conflicts(m.startKey.mapping()));
        });
        page(ctx, GlobalSettings.Language.ENGLISH, "page_en");
        page(ctx, GlobalSettings.Language.GERMAN, "page_de");
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeycamera")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = CinematicScene.build(ctx, world);
            CinematicScene.guest(ctx, o);
            CinematicScene.waypoint(ctx, o);
            ReviewKit.look(ctx, 200, 10);
            CinematicChecks.modes(ctx);
            CinematicChecks.watchFairness(ctx, Mode.MIX, 400);
            lyrics(ctx, true);
            CinematicAfkChecks.afkScreen(ctx, "cinematic.cinema", "afk_cinema_lyrics");
            CinematicAfkChecks.afkScreen(ctx, "cinematic.music", "afk_music_lyrics");
            lyrics(ctx, false);
            ReviewKit.cmd(world, "time set 12400");
            CinematicAfkChecks.afkScreen(ctx, "cinematic.minimal", "afk_minimal_sunset");
            CinematicAfkChecks.editor(ctx);
            CinematicAfkChecks.returnToPlayer(ctx);
            CinematicAfkChecks.triggers(ctx);
            ctx.runOnClient(mc -> CinematicModule.get().settings().all().forEach(s -> s.reset()));
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
    }

    private static void lyrics(ClientGameTestContext ctx, boolean on) {
        ctx.runOnClient(mc -> {
            if (on) {
                LyricsConfig.get().volume = 0;
                Playback.play(SongLibrary.find(SongLibrary.DEMO.toLowerCase(Locale.ROOT)));
                Playback.seek(25_350);
            } else {
                Playback.stop();
            }
        });
        ctx.waitTicks(10);
    }

    private static void page(ClientGameTestContext ctx, GlobalSettings.Language lang, String name) {
        ctx.runOnClient(mc -> GlobalSettings.language.set(lang));
        ctx.runOnClient(mc -> Menus.openModule(null, "cinematic"));
        ctx.waitTicks(15);
        KIT.shot(ctx, name);
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
    }
}

package dev.henny.vibeyclient.gametest.media;

import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.video.Video;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class MediaR3Run {
    private MediaR3Run() {}

    public static void run(ClientGameTestContext ctx) {
        if (FabricLoader.getInstance().isModLoaded("vibeylyrics")) return;
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        });
        Path first = Room.testVideo("bars.mp4", "testsrc2", 440);
        Path second = Room.testVideo("rings.mp4", "smptehdbars", 660);
        Check.log("test videos: %s, %s", first, second);
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("mediar3")).create()) {
            world.getClientWorld().waitForChunksRender();
            Room room = Room.build(ctx, world);
            Film.start();
            VideoSteps video = new VideoSteps(ctx, room);
            video.place(first);
            video.clickPlay();
            video.grabAndMove();
            video.wheel();
            video.edgeScale();
            video.lock();
            SourceSteps source = new SourceSteps(ctx, video);
            source.typeUrl(second);
            source.escapeReleases();
            source.moreMenu();
            MusicSteps music = new MusicSteps(ctx, room);
            music.place();
            music.buttons();
            music.pin();
            Film.stop();
            new LyricsSteps(ctx).visible();
            ctx.runOnClient(mc -> {
                Video.get().actions.removeAllOwn();
                Playback.stop();
            });
        }
        Check.log("done");
    }
}

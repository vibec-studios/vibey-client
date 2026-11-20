package dev.henny.vibeyclient.gametest.media;

import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.world.WorldText;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class LyricsSteps {
    private final ClientGameTestContext ctx;

    LyricsSteps(ClientGameTestContext ctx) {
        this.ctx = ctx;
    }

    void visible() {
        ctx.runOnClient(mc -> {
            LyricsConfig c = LyricsConfig.get();
            c.position = LyricsConfig.Position.SKY;
            c.skyMode = LyricsConfig.SkyMode.VIEW;
            c.volume = 0;
            if (Playback.song() == null) Playback.play(MusicSteps.demo());
            Playback.seek(25_350);
            Playback.pause();
            mc.player.setXRot(-25);
            mc.player.xRotO = -25;
        });
        long before = ctx.computeOnClient(mc -> WorldText.drawnFrames());
        Film.wait(ctx, 30);
        long after = ctx.computeOnClient(mc -> WorldText.drawnFrames());
        Check.that(after > before, "sky lyrics should be drawn through r3 (" + before + " -> " + after + ")");
        float[] view = ctx.computeOnClient(mc -> WorldText.viewState());
        Check.log("sky lyrics: drawn %d frames, view yaw %.1f elevation %.1f", after - before, view[0], view[1]);
        Film.shot(ctx, "17_lyrics_sky");
        ctx.runOnClient(mc -> {
            LyricsConfig.get().skyMode = LyricsConfig.SkyMode.SOFT;
            mc.player.setXRot(10);
            mc.player.xRotO = 10;
        });
        Film.wait(ctx, 30);
        Film.shot(ctx, "18_lyrics_soft_follow_lowered");
        ctx.runOnClient(mc -> {
            LyricsConfig.get().position = LyricsConfig.Position.FRONT;
            mc.player.setXRot(0);
            mc.player.xRotO = 0;
        });
        Film.wait(ctx, 30);
        Film.shot(ctx, "19_lyrics_front");
        ctx.runOnClient(mc -> {
            LyricsConfig c = LyricsConfig.get();
            c.position = LyricsConfig.Position.SKY;
            c.skyMode = LyricsConfig.SkyMode.VIEW;
            Playback.stop();
        });
    }
}

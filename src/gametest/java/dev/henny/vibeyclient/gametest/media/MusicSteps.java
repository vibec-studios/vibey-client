package dev.henny.vibeyclient.gametest.media;

import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.anchor.SoftFollowAnchor;
import dev.henny.r3.interact.InteractionState;
import dev.henny.r3.math.Face;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.library.Song;
import dev.henny.vibeyclient.module.media.lyrics.library.SongLibrary;
import dev.henny.vibeyclient.module.media.lyrics.world.MusicPanel;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.util.Locale;

final class MusicSteps {
    private final ClientGameTestContext ctx;
    private final Room room;

    MusicSteps(ClientGameTestContext ctx, Room room) {
        this.ctx = ctx;
        this.room = room;
    }

    static Song demo() {
        Song demo = SongLibrary.find(SongLibrary.DEMO.toLowerCase(Locale.ROOT));
        if (demo == null) throw new AssertionError("Demo song missing from " + SongLibrary.songs());
        return demo;
    }

    void place() {
        ctx.runOnClient(mc -> {
            LyricsConfig.get().volume = 0;
            Playback.play(demo());
            Playback.seek(25_350);
        });
        Aim.at(ctx, room.westWall(2.0, 0));
        ctx.runOnClient(mc -> MusicPanel.open());
        Film.wait(ctx, 4);
        Check.that(Check.state(ctx) == InteractionState.HOLDING, "the music panel follows the crosshair");
        Aim.glide(ctx, room.westWall(2.0, 0), room.westWall(1.8, -0.5), 5);
        ctx.getInput().pressMouse(VideoSteps.LEFT);
        Film.wait(ctx, 4);
        Face face = ctx.computeOnClient(mc -> MusicPanel.panel().anchor() instanceof BlockFaceAnchor a ? a.face() : null);
        Check.that(face == Face.EAST, "music panel on the west wall, face " + face);
        Film.shot(ctx, "14_music_panel");
    }

    void buttons() {
        WorldObject panel = ctx.computeOnClient(mc -> MusicPanel.panel());
        boolean playing = ctx.computeOnClient(mc -> Playback.isPlaying());
        click(panel, "play");
        Check.that(ctx.computeOnClient(mc -> Playback.isPlaying()) != playing, "▶/⏸ on the panel toggles the music");
        Film.shot(ctx, "15_music_toggled");
        click(panel, "play");
        click(panel, "next");
        click(panel, "previous");
        int presses = ctx.computeOnClient(mc -> MusicPanel.controls().presses());
        Check.that(presses == 4, "four panel presses, got " + presses);
    }

    void pin() {
        WorldObject panel = ctx.computeOnClient(mc -> MusicPanel.panel());
        Aim.at(ctx, Aim.point(panel, 0.2, 0.5));
        ctx.getInput().pressMouse(VideoSteps.RIGHT);
        Film.wait(ctx, 6);
        Check.that(ctx.computeOnClient(mc -> panel.anchor() instanceof SoftFollowAnchor), "right click pins it to the view");
        Aim.at(ctx, room.northWall(2, 1.5), 30);
        Film.shot(ctx, "16_music_pinned");
        ctx.runOnClient(mc -> MusicPanel.togglePinned());
        ctx.runOnClient(mc -> MusicPanel.close());
    }

    private void click(WorldObject panel, String id) {
        Aim.at(ctx, Aim.point(panel, 0.6, 0.3), 3);
        Aim.at(ctx, ctx.computeOnClient(mc -> Aim.control(panel, id)), 4);
        Check.that(id.equals(Check.control(ctx)), "crosshair on " + id + ", was " + Check.control(ctx));
        ctx.getInput().pressMouse(VideoSteps.LEFT);
        Film.wait(ctx, 5);
    }
}

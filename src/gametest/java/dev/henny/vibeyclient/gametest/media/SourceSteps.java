package dev.henny.vibeyclient.gametest.media;

import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.module.media.video.controls.ScreenControls;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.nio.file.Path;

final class SourceSteps {
    static final int ENTER = 257, ESCAPE = 256, KEY_W = 87;

    private final ClientGameTestContext ctx;
    private final VideoSteps video;

    SourceSteps(ClientGameTestContext ctx, VideoSteps video) {
        this.ctx = ctx;
        this.video = video;
    }

    void typeUrl(Path second) {
        MediaScreen screen = video.screen();
        video.clickControl("source");
        Film.wait(ctx, 4);
        WorldObject focused = ctx.computeOnClient(mc -> R3Mc.scene().keyTarget());
        Check.that(focused == video.object(), "the link field should take the keys");
        double z = ctx.computeOnClient(mc -> mc.player.getZ());
        ctx.getInput().holdKeyFor(KEY_W, 10);
        double z2 = ctx.computeOnClient(mc -> mc.player.getZ());
        Check.that(Math.abs(z2 - z) < 1e-3, "W while typing must not walk");
        ctx.runOnClient(mc -> ScreenControls.of(screen).url.set(""));
        Film.wait(ctx, 2);
        String text = second == null ? "https://example.org/video.mp4" : second.toString();
        ctx.getInput().typeChars(text);
        Film.wait(ctx, 4);
        Film.shot(ctx, "10_url_typed");
        String typed = ctx.computeOnClient(mc -> ScreenControls.of(screen).url.peek());
        Check.that(text.equals(typed), "typed into the field: '" + typed + "'");
        ctx.getInput().pressKey(ENTER);
        Film.wait(ctx, 6);
        String uri = ctx.computeOnClient(mc -> screen.source == null ? null : screen.source.uri());
        Check.that(text.equals(uri), "Enter should apply the link, source " + uri);
        Check.that(ctx.computeOnClient(mc -> R3Mc.scene().keyTarget()) == null, "applying releases the keys");
        Film.shot(ctx, "11_url_applied");
    }

    void escapeReleases() {
        video.clickControl("source");
        Film.wait(ctx, 4);
        Check.that(ctx.computeOnClient(mc -> R3Mc.scene().keyTarget()) != null, "field focused again");
        ctx.getInput().pressKey(ESCAPE);
        Film.wait(ctx, 3);
        Check.that(ctx.computeOnClient(mc -> R3Mc.scene().keyTarget()) == null, "Esc releases the keys");
        Check.that(ctx.computeOnClient(mc -> mc.screen == null), "Esc in the field must not open the pause menu");
        video.clickControl("cancelUrl");
    }

    void moreMenu() {
        MediaScreen screen = video.screen();
        Aim.at(ctx, Aim.point(video.object(), 0.5, 0.3));
        ctx.getInput().pressMouse(VideoSteps.RIGHT);
        Film.wait(ctx, 4);
        String mode = ctx.computeOnClient(mc -> ScreenControls.of(screen).mode.peek());
        Check.that(ScreenControls.MORE.equals(mode), "right click opens the menu, mode " + mode);
        Film.shot(ctx, "12_more_menu");
        video.clickControl("overlay");
        boolean overlay = ctx.computeOnClient(mc -> screen.id.equals(Video.get().overlayId));
        Check.that(overlay, "the overlay button shows it on the HUD");
        Film.shot(ctx, "13_overlay_on");
        video.clickControl("overlay");
        video.clickControl("closeMore");
    }
}

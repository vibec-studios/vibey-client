package dev.henny.vibeyclient.gametest.media;

import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.interact.HoverZone;
import dev.henny.r3.interact.InteractionState;
import dev.henny.r3.math.Face;
import dev.henny.r3.mc.R3Hooks;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.MediaSource;
import dev.henny.vibeyclient.module.media.video.Video;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.joml.Vector3d;

import java.nio.file.Path;

final class VideoSteps {
    static final int LEFT = 0, RIGHT = 1, MIDDLE = 2;

    private final ClientGameTestContext ctx;
    private final Room room;
    private MediaScreen screen;

    VideoSteps(ClientGameTestContext ctx, Room room) {
        this.ctx = ctx;
        this.room = room;
    }

    MediaScreen screen() {
        return screen;
    }

    WorldObject object() {
        return ctx.computeOnClient(mc -> screen.object);
    }

    void place(Path video) {
        Aim.at(ctx, room.northWall(0, 2.4));
        MediaSource src = video == null ? null : MediaSource.parse(video.toString());
        screen = ctx.computeOnClient(mc -> Video.get().actions.newScreen(src, 16f / 9f));
        Film.wait(ctx, 6);
        Check.that(Check.state(ctx) == InteractionState.HOLDING, "a new screen follows the crosshair, state " + Check.state(ctx));
        Aim.glide(ctx, room.northWall(0, 2.4), room.northWall(-0.5, 2.6), 6);
        ctx.getInput().pressMouse(LEFT);
        Film.wait(ctx, 4);
        Face face = ctx.computeOnClient(mc -> screen.object != null && screen.object.anchor() instanceof BlockFaceAnchor a ? a.face() : null);
        Check.that(face == Face.SOUTH, "screen should stick to the north wall, face " + face);
        if (video != null) waitForPicture();
        Film.shot(ctx, "01_screen_placed");
    }

    private void waitForPicture() {
        for (int i = 0; i < 100; i++) {
            boolean picture = ctx.computeOnClient(mc -> screen.player != null && screen.player.texture() != null);
            if (picture) return;
            Film.wait(ctx, 2);
        }
        String line = ctx.computeOnClient(mc -> Video.get().status.line(screen));
        Check.log("no picture after 200 ticks: %s", line);
    }

    void clickPlay() {
        Aim.at(ctx, Aim.point(object(), 0.5, 0.4), 6);
        Aim.at(ctx, ctx.computeOnClient(mc -> Aim.control(screen.object, "play")), 4);
        Check.that("play".equals(Check.control(ctx)), "crosshair should be on play, was " + Check.control(ctx));
        boolean outline = ctx.computeOnClient(mc -> R3Hooks.hidesBlockOutline());
        Check.that(outline, "the block outline must be hidden while a screen is hovered");
        Film.shot(ctx, "02_hover_play");
        boolean before = ctx.computeOnClient(mc -> screen.playing());
        ctx.getInput().pressMouse(LEFT);
        Film.wait(ctx, 6);
        boolean after = ctx.computeOnClient(mc -> screen.playing());
        Check.that(before != after, "clicking play/pause should toggle playback (" + before + ")");
        boolean focus = ctx.computeOnClient(mc -> Aim.document(screen.object).document().events().focus().focused() == null);
        Check.that(focus, "the clicked button must not keep the focus ring");
        Film.shot(ctx, "03_play_clicked");
        if (!after) {
            ctx.getInput().pressMouse(LEFT);
            Film.wait(ctx, 4);
        }
    }

    void grabAndMove() {
        Vector3d body = Aim.point(object(), 0.5, 0.3);
        Aim.at(ctx, body);
        ctx.getInput().holdMouse(MIDDLE);
        Film.wait(ctx, 4);
        Check.that(Check.state(ctx) == InteractionState.HOLDING, "middle mouse should grab, state " + Check.state(ctx));
        Aim.glide(ctx, body, room.eastWall(2.4, 0), 10);
        Film.shot(ctx, "04_moving");
        ctx.getInput().releaseMouse(MIDDLE);
        Film.wait(ctx, 4);
        Face face = ctx.computeOnClient(mc -> screen.object.anchor() instanceof BlockFaceAnchor a ? a.face() : null);
        Check.that(face == Face.WEST, "screen should snap to the east wall, face " + face);
        Film.shot(ctx, "05_moved_east_wall");
    }

    void wheel() {
        Aim.at(ctx, Aim.point(object(), 0.5, 0.3));
        float before = width();
        ctx.getInput().scroll(1);
        Film.wait(ctx, 3);
        ctx.getInput().scroll(1);
        Film.wait(ctx, 4);
        float after = width();
        Check.log("wheel: %.3f -> %.3f", before, after);
        Check.that(after > before + 0.3f, "two wheel notches should grow the screen");
        Film.shot(ctx, "06_wheel_bigger");
    }

    void edgeScale() {
        Vector3d edge = ctx.computeOnClient(mc -> Aim.rightEdge(screen.object, 0.0));
        Aim.at(ctx, edge);
        Check.that(Check.zone(ctx) == HoverZone.EDGE, "crosshair on the border should be EDGE, was " + Check.zone(ctx));
        Film.shot(ctx, "07_edge_hover");
        Vector3d fixed = ctx.computeOnClient(mc -> Aim.leftEdge(screen.object));
        float before = width();
        ctx.getInput().holdMouse(MIDDLE);
        Film.wait(ctx, 3);
        Check.that(Check.state(ctx) == InteractionState.RESIZING, "middle on the border should resize, state " + Check.state(ctx));
        Aim.glide(ctx, edge, ctx.computeOnClient(mc -> Aim.rightEdge(screen.object, 0.8)), 6);
        ctx.getInput().releaseMouse(MIDDLE);
        Film.wait(ctx, 4);
        float after = width();
        double moved = ctx.computeOnClient(mc -> Aim.leftEdge(screen.object).distance(fixed));
        Check.log("edge: %.3f -> %.3f, opposite edge moved %.4f", before, after, moved);
        Check.that(after > before + 0.3f, "dragging the border should widen the screen");
        Check.that(moved < 0.05, "the opposite edge should stay, moved " + moved);
        Film.shot(ctx, "08_edge_scaled");
    }

    void lock() {
        clickControl("lock");
        boolean locked = ctx.computeOnClient(mc -> screen.object.lockedByUser());
        Check.that(locked, "lock button should lock the screen");
        Film.shot(ctx, "09_locked");
        Aim.at(ctx, Aim.point(object(), 0.5, 0.3));
        ctx.getInput().holdMouse(MIDDLE);
        Film.wait(ctx, 3);
        Check.that(Check.state(ctx) == InteractionState.IDLE, "a locked screen must not be grabbed, state " + Check.state(ctx));
        ctx.getInput().releaseMouse(MIDDLE);
        clickControl("lock");
        Check.that(!ctx.computeOnClient(mc -> screen.object.lockedByUser()), "second click should unlock");
    }

    void clickControl(String id) {
        Aim.at(ctx, Aim.point(object(), 0.5, 0.4), 4);
        Aim.at(ctx, ctx.computeOnClient(mc -> Aim.control(screen.object, id)), 4);
        Check.that(id.equals(Check.control(ctx)), "crosshair should be on " + id + ", was " + Check.control(ctx));
        ctx.getInput().pressMouse(LEFT);
        Film.wait(ctx, 4);
    }

    float width() {
        return ctx.computeOnClient(mc -> screen.object.transform().worldWidth());
    }
}

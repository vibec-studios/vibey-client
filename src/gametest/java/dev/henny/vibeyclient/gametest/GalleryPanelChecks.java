package dev.henny.vibeyclient.gametest;

import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.interact.InteractionState;
import dev.henny.r3.math.Face;
import dev.henny.r3.mc.R3Mc;
import dev.henny.vibeyclient.module.clips.shots.Shots;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.joml.Vector3d;

import static dev.henny.vibeyclient.gametest.GalleryAim.clickScreen;
import static dev.henny.vibeyclient.gametest.GalleryAim.panelElement;
import static dev.henny.vibeyclient.gametest.GalleryAim.panelPoint;
import static dev.henny.vibeyclient.gametest.GalleryGameTest.KIT;
import static dev.henny.vibeyclient.gametest.GalleryGameTest.c;
import static dev.henny.vibeyclient.gametest.GalleryGameTest.selected;
import static dev.henny.vibeyclient.gametest.GalleryGameTest.waitFor;
import static dev.henny.vibeyclient.gametest.ReviewKit.LEFT;
import static dev.henny.vibeyclient.gametest.ReviewKit.MIDDLE;
import static dev.henny.vibeyclient.gametest.ReviewKit.RIGHT;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;

final class GalleryPanelChecks {
    private GalleryPanelChecks() {}

    static void panel(ClientGameTestContext ctx, int[] o) {
        ReviewKit.lookAt(ctx, new Vector3d(o[0] + 0.5, o[1] + 1.8, o[2] - 4.5));
        ctx.waitTicks(140);
        clickScreen(ctx, "#world", 0);
        String state = ctx.computeOnClient(mc -> "screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName())
                + " panel=" + (Shots.panel() != null));
        check(state.equals("screen=none panel=true"), "world button closes the screen and places: " + state);
        ctx.waitTicks(4);
        ctx.getInput().pressMouse(LEFT);
        ctx.waitTicks(6);
        Face face = ctx.computeOnClient(mc -> Shots.panel().anchor() instanceof BlockFaceAnchor a ? a.face() : null);
        check(face == Face.SOUTH, "panel should stick to the north wall, face " + face);
        ReviewKit.lookAt(ctx, ctx.computeOnClient(mc -> panelPoint(0.5, 0.4)));
        waitFor(ctx, "panel preview", 200, () -> !c().preview.peek().isEmpty());
        KIT.shot(ctx, "panel_placed");
        lookToBrowse(ctx);
        clickNext(ctx);
        grabAndResize(ctx, o);
        ReviewKit.lookAt(ctx, ctx.computeOnClient(mc -> panelPoint(0.5, 0.4)));
        ctx.getInput().pressMouse(RIGHT);
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> Shots.panel() == null), "right click closes the panel");
        KIT.shot(ctx, "panel_closed");
    }

    private static void lookToBrowse(ClientGameTestContext ctx) {
        String before = ctx.computeOnClient(mc -> selected());
        int target = ctx.computeOnClient(mc -> c().strip.get(0).id.equals(before) ? 1 : 0);
        String want = ctx.computeOnClient(mc -> c().strip.get(target).id);
        ReviewKit.lookAt(ctx, ctx.computeOnClient(mc -> panelElement(".sp-thumb", target)));
        waitFor(ctx, "dwell select", 60, () -> want.equals(selected()));
        waitFor(ctx, "panel preview", 200, () -> !c().preview.peek().isEmpty());
        KIT.log("look to browse: %s -> %s", before, want);
        KIT.shot(ctx, "panel_look_browse");
    }

    private static void clickNext(ClientGameTestContext ctx) {
        String before = ctx.computeOnClient(mc -> selected());
        ReviewKit.lookAt(ctx, ctx.computeOnClient(mc -> panelElement("#next", 0)));
        String hovered = ctx.computeOnClient(mc -> R3Mc.scene().interaction().hover().controlId());
        check("next".equals(hovered), "crosshair should hover next, hovered " + hovered);
        ctx.getInput().pressMouse(LEFT);
        ctx.waitTicks(4);
        String after = ctx.computeOnClient(mc -> selected());
        check(after != null && !after.equals(before), "left click on next steps the gallery");
        KIT.shot(ctx, "panel_next_clicked");
    }

    private static void grabAndResize(ClientGameTestContext ctx, int[] o) {
        Vector3d body = ctx.computeOnClient(mc -> panelPoint(0.5, 0.4));
        ReviewKit.lookAt(ctx, body);
        ctx.getInput().holdMouse(MIDDLE);
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> R3Mc.scene().interaction().state()) == InteractionState.HOLDING, "middle grabs the panel");
        Vector3d east = new Vector3d(o[0] + 5, o[1] + 1.8, o[2] + 0.5);
        for (int i = 1; i <= 8; i++) ReviewKit.lookAt(ctx, new Vector3d(body).lerp(east, i / 8.0));
        ctx.getInput().releaseMouse(MIDDLE);
        ctx.waitTicks(4);
        Face face = ctx.computeOnClient(mc -> Shots.panel().anchor() instanceof BlockFaceAnchor a ? a.face() : null);
        check(face == Face.WEST, "middle-drag moves the panel to the east wall, face " + face);
        KIT.shot(ctx, "panel_moved_east");
        ReviewKit.lookAt(ctx, ctx.computeOnClient(mc -> panelPoint(0.5, 0.4)));
        float before = ctx.computeOnClient(mc -> Shots.panel().transform().worldWidth());
        ctx.getInput().scroll(1);
        ctx.waitTicks(2);
        ctx.getInput().scroll(1);
        ctx.waitTicks(4);
        float after = ctx.computeOnClient(mc -> Shots.panel().transform().worldWidth());
        KIT.log("wheel: width %.2f -> %.2f", before, after);
        check(after > before + 0.3f, "wheel makes the panel bigger");
        KIT.shot(ctx, "panel_wheel_bigger");
    }
}

package dev.henny.vibeyclient.gametest;

import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.interact.InteractionState;
import dev.henny.r3.interact.HoverZone;
import dev.henny.r3.math.Face;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.demo.R3Demo;
import dev.henny.r3.object.Transform;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;

import java.util.List;
import java.util.Locale;

public final class R3GameTest implements FabricClientGameTest {
    private static final String P = "[r3-gametest] ";
    private static final int LEFT = 0, MIDDLE = 2, ESCAPE = 256;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("r3demo")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = room(ctx, world);
            placeDemo(ctx, world, o);
            clickPlay(ctx, o);
            grabAndSnap(ctx, o);
            wheel(ctx);
            edgeResize(ctx);
            escapeCancels(ctx, o);
            ctx.runOnClient(mc -> R3Demo.stop());
        }
    }

    private static int[] room(ClientGameTestContext ctx, TestSingleplayerContext world) {
        int[] p = ctx.computeOnClient(mc -> new int[] {(int) Math.floor(mc.player.getX()), 120, (int) Math.floor(mc.player.getZ())});
        int x = p[0], y = p[1], z = p[2];
        for (String c : new String[] {"gamemode creative @a", "time set 6000", "weather clear", "gamerule fallDamage false",
                "gamerule doDaylightCycle false"}) world.getServer().runCommand(c);
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 6, y, z - 6, x + 6, y + 6, z + 6);
        cmd(world, "fill %d %d %d %d %d %d minecraft:smooth_stone", x - 6, y - 1, z - 6, x + 6, y - 1, z + 6);
        cmd(world, "fill %d %d %d %d %d %d minecraft:white_concrete", x - 4, y, z - 4, x + 4, y + 5, z - 4);
        cmd(world, "fill %d %d %d %d %d %d minecraft:light_gray_concrete", x + 4, y, z - 4, x + 4, y + 5, z + 4);
        cmd(world, "tp @a %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5);
        ctx.waitTicks(30);
        world.getClientWorld().waitForChunksRender();
        return new int[] {x, y, z};
    }

    private static void placeDemo(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        R3TestAim.lookAt(ctx, new Vector3d(o[0] + 0.5, o[1] + 1.6, o[2] - 3));
        ctx.runOnClient(mc -> mc.player.connection.sendCommand("r3 demo"));
        ctx.waitTicks(6);
        boolean placed = ctx.computeOnClient(mc -> R3Demo.panel() != null);
        check(placed, "/r3 demo placed no panel");
        Face face = ctx.computeOnClient(mc -> ((BlockFaceAnchor) R3Demo.panel().anchor()).face());
        check(face == Face.SOUTH, "demo panel should be on the north wall's south face, was " + face);
        R3TestAim.shot(ctx, "01_demo_placed");
    }

    private static void clickPlay(ClientGameTestContext ctx, int[] o) {
        Vector3d play = ctx.computeOnClient(mc -> R3TestAim.control("play"));
        R3TestAim.lookAt(ctx, play);
        String hovered = ctx.computeOnClient(mc -> R3Mc.scene().interaction().hover().controlId());
        check("play".equals(hovered), "crosshair should hover the play button, hovered " + hovered);
        R3TestAim.shot(ctx, "02_hover_play");
        ctx.getInput().pressMouse(LEFT);
        ctx.waitTicks(4);
        int plays = ctx.computeOnClient(mc -> R3Demo.controller().plays());
        check(plays == 1, "play should fire once, fired " + plays);
        check(actions(ctx).contains("play"), "ACTION play missing: " + actions(ctx));
        boolean wall = ctx.computeOnClient(mc -> !mc.level.getBlockState(BlockPos.containing(play.x, play.y, play.z - 0.5)).isAir());
        check(wall, "the left click reached vanilla and broke the wall block");
        R3TestAim.shot(ctx, "03_play_clicked");
    }

    private static void grabAndSnap(ClientGameTestContext ctx, int[] o) {
        Vector3d body = ctx.computeOnClient(mc -> R3TestAim.panelPoint(0.5, 0.2));
        R3TestAim.lookAt(ctx, body);
        ctx.getInput().holdMouse(MIDDLE);
        ctx.waitTicks(4);
        check(state(ctx) == InteractionState.HOLDING, "middle mouse should grab, state " + state(ctx));
        R3TestAim.shot(ctx, "04_grabbed");
        Vector3d east = new Vector3d(o[0] + 4, o[1] + 1.6, o[2] + 0.5);
        R3TestAim.turn(ctx, body, east, 8);
        R3TestAim.shot(ctx, "05_following");
        ctx.getInput().releaseMouse(MIDDLE);
        ctx.waitTicks(4);
        Face face = ctx.computeOnClient(mc -> R3Demo.panel().anchor() instanceof BlockFaceAnchor a ? a.face() : null);
        check(face == Face.WEST, "panel should snap to the east wall (west face), was " + face);
        check(actions(ctx).contains("PLACED") || actions(ctx).contains("MOVED"), "no PLACED/MOVED: " + actions(ctx));
        R3TestAim.shot(ctx, "06_snapped_east_wall");
    }

    private static void wheel(ClientGameTestContext ctx) {
        R3TestAim.lookAt(ctx, ctx.computeOnClient(mc -> R3TestAim.panelPoint(0.5, 0.2)));
        float before = width(ctx);
        ctx.getInput().scroll(1);
        ctx.waitTicks(2);
        ctx.getInput().scroll(1);
        ctx.waitTicks(4);
        float after = width(ctx);
        log("wheel: %.3f -> %.3f", before, after);
        check(after > before + 0.4f, "two wheel notches should grow the panel");
        R3TestAim.shot(ctx, "07_wheel_bigger");
    }

    private static void edgeResize(ClientGameTestContext ctx) {
        R3TestAim.lookAt(ctx, ctx.computeOnClient(mc -> R3TestAim.edge(0.0)));
        HoverZone zone = ctx.computeOnClient(mc -> R3Mc.scene().interaction().hover().zone());
        check(zone == HoverZone.EDGE, "crosshair on the border should be EDGE, was " + zone);
        R3TestAim.shot(ctx, "08_edge_hover");
        Vector3d fixed = ctx.computeOnClient(mc -> R3TestAim.oppositeEdge());
        float before = width(ctx);
        Vector3d start = ctx.computeOnClient(mc -> R3TestAim.edge(0.0));
        ctx.getInput().holdMouse(MIDDLE);
        ctx.waitTicks(3);
        check(state(ctx) == InteractionState.RESIZING, "middle on the border should resize, state " + state(ctx));
        R3TestAim.turn(ctx, start, ctx.computeOnClient(mc -> R3TestAim.edge(0.6)), 4);
        ctx.getInput().releaseMouse(MIDDLE);
        ctx.waitTicks(4);
        float after = width(ctx);
        double moved = ctx.computeOnClient(mc -> R3TestAim.oppositeEdge().distance(fixed));
        log("edge: width %.3f -> %.3f, opposite edge moved %.4f", before, after, moved);
        check(after > before + 0.3f, "dragging the edge should widen the panel");
        check(moved < 0.02, "the opposite edge should stay fixed, moved " + moved);
        R3TestAim.shot(ctx, "09_edge_resized");
    }

    private static void escapeCancels(ClientGameTestContext ctx, int[] o) {
        Vector3d body = ctx.computeOnClient(mc -> R3TestAim.panelPoint(0.5, 0.2));
        Vector3d before = ctx.computeOnClient(mc -> new Vector3d(R3Demo.panel().transform().position()));
        R3TestAim.lookAt(ctx, body);
        ctx.getInput().holdMouse(MIDDLE);
        ctx.waitTicks(3);
        R3TestAim.turn(ctx, body, new Vector3d(o[0] + 0.5, o[1] + 1.6, o[2] - 4), 6);
        R3TestAim.shot(ctx, "10_grabbed_before_esc");
        ctx.getInput().pressKey(ESCAPE);
        ctx.waitTicks(4);
        boolean noPause = ctx.computeOnClient(mc -> mc.screen == null);
        check(noPause, "Esc while holding must not open the pause menu");
        ctx.getInput().releaseMouse(MIDDLE);
        ctx.waitTicks(3);
        double back = ctx.computeOnClient(mc -> R3Demo.panel().transform().position().distance(before));
        check(back < 1e-3, "Esc should put the panel back, off by " + back);
        check(actions(ctx).contains("MOVE_CANCELLED"), "no MOVE_CANCELLED: " + actions(ctx));
        R3TestAim.lookAt(ctx, body);
        R3TestAim.shot(ctx, "11_esc_cancelled");
        log("actions: %s", actions(ctx));
    }

    private static List<String> actions(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> List.copyOf(R3Demo.actions()));
    }

    private static InteractionState state(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> R3Mc.scene().interaction().state());
    }

    private static float width(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            Transform t = R3Demo.panel().transform();
            return t.worldWidth();
        });
    }

    private static void cmd(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(String.format(Locale.ROOT, format, args));
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(P + message);
    }

    private static void log(String format, Object... args) {
        System.out.println(P + String.format(Locale.ROOT, format, args));
    }
}

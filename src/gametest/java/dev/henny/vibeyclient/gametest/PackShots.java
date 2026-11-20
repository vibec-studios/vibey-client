package dev.henny.vibeyclient.gametest;

import dev.henny.companio.mc.CompanioMc;
import dev.henny.companio.world.Companion;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.CompanionTestKit.*;

final class PackShots {
    static final ReviewShots SHOTS = new ReviewShots("companion-packs");

    private PackShots() {}

    static void snap(ClientGameTestContext ctx, String name) {
        SHOTS.shot(ctx, name);
    }

    static void select(ClientGameTestContext ctx, String key) {
        waitFor(ctx, "packs", 400, () -> CompanioMc.companions().stream().anyMatch(d -> (d.packId() + ":" + d.id()).equals(key)));
        ctx.runOnClient(mc -> {
            CompanioMc.config().size = 0;
            check(CompanioMc.select(key), key + " is a built-in companion");
        });
        waitFor(ctx, key, 100, () -> CompanioMc.own() != null && (CompanioMc.own().def.packId() + ":" + CompanioMc.own().def.id()).equals(key));
        ctx.waitTicks(30);
    }

    static void variant(ClientGameTestContext ctx, String v) {
        ctx.runOnClient(mc -> {
            CompanioMc.config().variant = v;
            CompanioMc.changed();
        });
        waitFor(ctx, "variant " + v, 60, () -> v.equals(CompanioMc.own().variant()));
    }

    static void force(ClientGameTestContext ctx, String behavior) {
        for (int t = 0; t < 30; t++) {
            boolean ok = ctx.computeOnClient(mc -> {
                for (int i = 0; i < 4000; i++) if (CompanioMc.own().brain().trick(behavior)) return true;
                return false;
            });
            if (ok) return;
            ctx.waitTicks(5);
        }
        check(false, "behavior " + behavior + " can start, running " + behavior(ctx));
    }

    static void waitBehavior(ClientGameTestContext ctx, String id, int ticks) {
        waitFor(ctx, "behavior " + id, ticks, () -> {
            Companion c = CompanioMc.own();
            return c.brain().current() != null && c.brain().current().def.id().equals(id);
        });
    }

    static void pet(ClientGameTestContext ctx) {
        for (int i = 0; i < 4; i++) {
            lookAt(ctx, center(ctx));
            waitFor(ctx, "hover", 60, () -> dev.henny.companio.mc.client.CompanioClient.get().target().hovered());
            ctx.getInput().pressMouse(LEFT);
            ctx.waitTicks(4);
            if ("petted".equals(behavior(ctx))) return;
            ctx.waitTicks(20);
        }
        check(false, "left click pets the companion, behavior " + behavior(ctx));
    }

    static void face(ClientGameTestContext ctx, TestSingleplayerContext world, double dist, double side, double up) {
        Companion s = own(ctx);
        double fx = -Math.sin(Math.toRadians(s.body.yaw)), fz = Math.cos(Math.toRadians(s.body.yaw));
        tp(world, ctx, s.body.x + fx * dist + fz * side, s.body.y + up, s.body.z + fz * dist - fx * side, 0, 20);
        ctx.waitTicks(4);
        lookAt(ctx, center(ctx).add(0, 0.15, 0));
    }
}

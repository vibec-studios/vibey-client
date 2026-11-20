package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.visuals.core.ItemTumble;
import dev.henny.vibeyclient.module.visuals.items.ItemMotionHolder;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Vector3d;

import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;
import static dev.henny.vibeyclient.gametest.VisualsGameTest.KIT;
import static dev.henny.vibeyclient.gametest.VisualsGameTest.m;

final class VisualsItemChecks {
    private static final String[] ITEMS = {"diamond", "stone", "iron_sword", "apple\",count:64", "oak_log", "bread",
            "golden_apple", "glass", "torch", "emerald\",count:20"};

    private VisualsItemChecks() {}

    static void items(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        ReviewKit.lookAt(ctx, new Vector3d(o[0] + 0.5, o[1] + 0.2, o[2] - 3.5));
        drop(world, o, 0.25);
        ctx.waitTicks(4);
        KIT.shotNow(ctx, "items_falling_physics");
        ctx.waitTicks(60);
        int[] st = ctx.computeOnClient(mc -> settled(mc.level.entitiesForRendering()));
        KIT.log("item physics: %d items, %d on the ground, %d settled flat/on a side", st[0], st[1], st[2]);
        check(st[0] >= ITEMS.length, "items should be there, found " + st[0]);
        check(st[2] >= st[1] - 1 && st[1] >= ITEMS.length - 1, "items on the ground should settle, " + st[2] + "/" + st[1]);
        KIT.shot(ctx, "items_on_ground_physics");
        ctx.runOnClient(mc -> m().itemPhysics.set(false));
        ctx.waitTicks(4);
        KIT.shot(ctx, "items_on_ground_vanilla");
        cmd(world, "kill @e[type=item]");
        drop(world, o, 0.25);
        ctx.waitTicks(4);
        KIT.shotNow(ctx, "items_falling_vanilla");
        ctx.runOnClient(mc -> m().itemPhysics.set(true));
        cmd(world, "kill @e[type=item]");
        ctx.waitTicks(4);
    }

    static void frameTime(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        for (int i = 0; i < 6; i++) drop(world, new int[] {o[0] - 3 + i, o[1], o[2] - 1}, 0.1);
        ctx.waitTicks(80);
        ReviewKit.lookAt(ctx, new Vector3d(o[0] + 0.5, o[1], o[2] - 3.5));
        int n = ctx.computeOnClient(mc -> count(mc.level.entitiesForRendering()));
        KIT.log("frame time with %d dropped items", n);
        ReviewKit.realFrameRate(ctx);
        KIT.compare(ctx, "item physics (" + n + " items)", 3, on -> m().itemPhysics.set(on));
        ReviewKit.normalFrameRate(ctx);
        cmd(world, "kill @e[type=item]");
    }

    private static void drop(TestSingleplayerContext world, int[] o, double up) {
        for (int i = 0; i < ITEMS.length; i++) {
            double x = o[0] + 0.5 + (i % 5 - 2) * 0.9, z = o[2] - 3.0 - (i / 5) * 1.2;
            cmd(world, "summon minecraft:item %.2f %d %.2f {Item:{id:\"minecraft:%s},PickupDelay:32767,Motion:[%.2f,%.2f,%.2f]}",
                    x, o[1] + 2, z, ITEMS[i].contains("\"") ? ITEMS[i] : ITEMS[i] + "\"", (i % 3 - 1) * 0.05, up, 0.03);
        }
    }

    private static int count(Iterable<Entity> all) {
        int n = 0;
        for (Entity e : all) if (e instanceof ItemEntity) n++;
        return n;
    }

    private static int[] settled(Iterable<Entity> all) {
        int items = 0, ground = 0, done = 0;
        for (Entity e : all) {
            if (!(e instanceof ItemEntity item)) continue;
            items++;
            if (!item.onGround()) continue;
            ground++;
            ItemTumble t = ((ItemMotionHolder) item).vibey$motion().tumble();
            if (t.settled()) done++;
        }
        return new int[] {items, ground, done};
    }
}

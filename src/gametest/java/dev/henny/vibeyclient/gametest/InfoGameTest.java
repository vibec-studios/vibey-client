package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.InfoRules;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.InfoTestKit.check;
import static dev.henny.vibeyclient.gametest.InfoTestKit.cmd;
import static dev.henny.vibeyclient.gametest.InfoTestKit.m;
import static dev.henny.vibeyclient.gametest.InfoTestKit.shot;

public final class InfoGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            InfoModule m = m();
            check(m != null && m.isEnabled(), "info module should be on");
            for (String key : InfoRules.ALL) check(ServerRules.supportedKeys().contains(key), "rule key not declared: " + key);
            m.settings().all().forEach(s -> s.reset());
        });
        page(ctx, GlobalSettings.Language.ENGLISH, "page_en");
        page(ctx, GlobalSettings.Language.GERMAN, "page_de");
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeyinfo")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = scene(ctx, world);
            InfoWorldChecks.light(ctx, world, o);
            InfoWorldChecks.chunks(ctx, o);
            InfoWorldChecks.tnt(ctx, world, o);
            InfoCardChecks.blocks(ctx, world, o);
            InfoCardChecks.mob(ctx, world, o);
            InfoCardChecks.pin(ctx, world, o);
            InfoHudChecks.tool(ctx, world);
            InfoHudChecks.food(ctx, world);
            InfoHudChecks.rules(ctx, o);
            ctx.runOnClient(mc -> m().settings().all().forEach(s -> s.reset()));
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
    }

    private static void page(ClientGameTestContext ctx, GlobalSettings.Language lang, String name) {
        ctx.runOnClient(mc -> GlobalSettings.language.set(lang));
        ModulesPanel.preselect("info");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        shot(ctx, name);
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
    }

    private static int[] scene(ClientGameTestContext ctx, TestSingleplayerContext world) {
        int[] p = ctx.computeOnClient(mc -> new int[] {(int) Math.floor(mc.player.getX()), 120, (int) Math.floor(mc.player.getZ())});
        int x = p[0], y = p[1], z = p[2];
        for (String c : new String[] {"gamemode creative @a", "time set 6000", "weather clear", "gamerule fallDamage false",
                "gamerule doDaylightCycle false", "gamerule doMobSpawning false", "difficulty peaceful"}) cmd(world, c);
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 7, y, z - 7, x + 7, y + 6, z + 7);
        cmd(world, "fill %d %d %d %d %d %d minecraft:stone", x - 7, y - 1, z - 7, x + 7, y - 1, z + 7);
        cmd(world, "setblock %d %d %d minecraft:diamond_ore", x - 1, y, z - 2);
        cmd(world, "setblock %d %d %d minecraft:farmland", x + 1, y - 1, z - 2);
        cmd(world, "setblock %d %d %d minecraft:wheat[age=3]", x + 1, y, z - 2);
        cmd(world, "setblock %d %d %d minecraft:farmland", x + 2, y - 1, z - 2);
        cmd(world, "setblock %d %d %d minecraft:wheat[age=7]", x + 2, y, z - 2);
        cmd(world, "tp @a %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5);
        ctx.waitTicks(30);
        world.getClientWorld().waitForChunksRender();
        return new int[] {x, y, z};
    }
}

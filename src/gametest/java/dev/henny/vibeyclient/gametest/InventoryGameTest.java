package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.inventory.InventoryModule;
import dev.henny.vibeyclient.module.inventory.InventoryRules;
import dev.henny.vibeyclient.module.inventory.presets.PresetHud;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.InvTestKit.check;
import static dev.henny.vibeyclient.gametest.InvTestKit.cmd;
import static dev.henny.vibeyclient.gametest.InvTestKit.m;
import static dev.henny.vibeyclient.gametest.InvTestKit.shot;

public final class InventoryGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            InventoryModule m = m();
            check(m != null && m.isEnabled(), "inventory module should be on");
            for (String key : InventoryRules.ALL) check(ServerRules.supportedKeys().contains(key), "rule key not declared: " + key);
            m.settings().all().forEach(s -> s.reset());
            check(!m.tweaks.get(), "quick moves must be off by default");
            check(!HudLayout.hidden(HudLayout.get(PresetHud.ID)), "preset bar hidden by the HUD template");
            m.tweaks.set(true);
        });
        page(ctx, GlobalSettings.Language.ENGLISH, "page_en");
        page(ctx, GlobalSettings.Language.GERMAN, "page_de");
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeyinventory")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = scene(ctx, world);
            InventoryTooltipChecks.run(ctx, world);
            InventoryTweakChecks.run(ctx, world, o);
            InventoryPresetChecks.run(ctx, world);
            InventoryTweakChecks.rule(ctx, world, o);
            ctx.runOnClient(mc -> m().settings().all().forEach(s -> s.reset()));
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
    }

    private static void page(ClientGameTestContext ctx, GlobalSettings.Language lang, String name) {
        ctx.runOnClient(mc -> GlobalSettings.language.set(lang));
        ModulesPanel.preselect("inventory");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        shot(ctx, name);
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
    }

    private static int[] scene(ClientGameTestContext ctx, TestSingleplayerContext world) {
        int[] p = ctx.computeOnClient(mc -> new int[] {(int) Math.floor(mc.player.getX()), 120, (int) Math.floor(mc.player.getZ())});
        int x = p[0], y = p[1], z = p[2];
        for (String c : new String[] {"gamemode survival @a", "time set 6000", "weather clear", "gamerule fall_damage false",
                "gamerule advance_time false", "gamerule spawn_mobs false", "difficulty peaceful", "clear @a"}) cmd(world, c);
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 4, y, z - 4, x + 4, y + 4, z + 4);
        cmd(world, "fill %d %d %d %d %d %d minecraft:stone", x - 4, y - 1, z - 4, x + 4, y - 1, z + 4);
        cmd(world, "tp @a %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5);
        ctx.waitTicks(30);
        world.getClientWorld().waitForChunksRender();
        return new int[] {x, y, z};
    }
}

package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.info.card.CardState;
import dev.henny.vibeyclient.module.info.core.FoodGain;
import dev.henny.vibeyclient.module.info.core.WarnLevel;
import dev.henny.vibeyclient.module.info.food.FoodState;
import dev.henny.vibeyclient.module.info.tool.ToolState;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import org.joml.Vector3d;

import java.util.List;

import static dev.henny.vibeyclient.gametest.InfoTestKit.check;
import static dev.henny.vibeyclient.gametest.InfoTestKit.cmd;
import static dev.henny.vibeyclient.gametest.InfoTestKit.log;
import static dev.henny.vibeyclient.gametest.InfoTestKit.lookAt;
import static dev.henny.vibeyclient.gametest.InfoTestKit.m;
import static dev.henny.vibeyclient.gametest.InfoTestKit.shot;

final class InfoHudChecks {
    private InfoHudChecks() {}

    static void tool(ClientGameTestContext ctx, TestSingleplayerContext world) {
        cmd(world, "gamemode survival @a");
        cmd(world, "item replace entity @a weapon.mainhand with minecraft:diamond_pickaxe[damage=1450]");
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> ToolState.level()) == WarnLevel.LOW, "111 uses left: low warning");
        shot(ctx, "tool_warning_low");
        cmd(world, "item replace entity @a weapon.mainhand with minecraft:diamond_pickaxe[damage=1555]");
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> ToolState.level()) == WarnLevel.CRITICAL, "6 uses left: critical warning");
        shot(ctx, "tool_warning_critical");
        ctx.waitTicks(4);
        shot(ctx, "tool_warning_critical_flash");
        cmd(world, "item replace entity @a weapon.mainhand with minecraft:diamond_pickaxe");
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> ToolState.level()) == WarnLevel.NONE, "a new tool: no warning");
    }

    static void food(ClientGameTestContext ctx, TestSingleplayerContext world) {
        cmd(world, "difficulty easy");
        world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().forEach(p -> {
            p.getFoodData().setFoodLevel(8);
            p.getFoodData().setSaturation(1f);
        }));
        cmd(world, "item replace entity @a hotbar.0 with minecraft:cooked_beef 4");
        cmd(world, "item replace entity @a hotbar.2 with minecraft:bread 4");
        ctx.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
        ctx.waitTicks(6);
        FoodGain held = ctx.computeOnClient(mc -> FoodState.gain());
        int now = ctx.computeOnClient(mc -> FoodState.food());
        check(held != null && held.foodAdded() == 8 && held.food() == now + 8, "steak: +8 food, was " + held + " on " + now);
        shot(ctx, "food_hold_steak");
        ctx.runOnClient(mc -> m().saturation.set(true));
        ctx.waitTicks(3);
        shot(ctx, "food_hold_steak_saturation");
        ctx.runOnClient(mc -> m().saturation.reset());

        ctx.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(4));
        ctx.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
        ctx.waitTicks(4);
        hover(ctx, 2);
        FoodGain bread = ctx.computeOnClient(mc -> FoodState.gain());
        log("hover bread: %s", bread);
        check(bread != null && bread.foodAdded() == 5, "hovering bread in the inventory should show +5");
        shot(ctx, "food_hover_bread_inventory");
        ctx.setScreen(() -> null);
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> FoodState.gain()) == null, "empty hand, no screen: no food preview");
        cmd(world, "difficulty peaceful");
    }

    private static void hover(ClientGameTestContext ctx, int hotbarSlot) {
        double[] at = ctx.computeOnClient(mc -> {
            AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) mc.screen;
            Slot slot = s.getMenu().slots.stream().filter(sl -> sl.getItem().is(Items.BREAD)).findFirst().orElseThrow();
            double scale = mc.getWindow().getGuiScale();
            int left = field(s, "leftPos"), top = field(s, "topPos");
            return new double[] {(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
        });
        ctx.getInput().setCursorPos(at[0], at[1]);
        ctx.waitTicks(4);
    }

    private static int field(Object o, String name) {
        try {
            var f = AbstractContainerScreen.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(o);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    static void rules(ClientGameTestContext ctx, int[] o) {
        Vector3d ore = new Vector3d(o[0] - 0.5, o[1] + 0.5, o[2] - 1.02);
        lookAt(ctx, ore);
        check(ctx.computeOnClient(mc -> CardState.current()) != null, "card before the rule");
        ctx.runOnClient(mc -> ServerRules.apply(List.of("info.*"), "No info overlays here"));
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> CardState.current()) == null, "info.* must hide the card");
        check(!ctx.computeOnClient(mc -> m().foodActive() || m().tntActive() || m().lightActive()), "info.* must stop all");
        shot(ctx, "rule_info_off_world");
        ModulesPanel.preselect("info");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        shot(ctx, "rule_info_off_page");
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> ServerRules.clear());
        ctx.waitTicks(4);
        lookAt(ctx, ore);
        check(ctx.computeOnClient(mc -> CardState.current()) != null, "card back after the rules are cleared");
    }
}

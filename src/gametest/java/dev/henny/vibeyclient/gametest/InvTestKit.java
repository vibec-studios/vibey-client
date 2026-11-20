package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.module.inventory.InventoryModule;
import dev.henny.vibeyclient.module.inventory.mixin.ContainerScreenAccess;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.Locale;

final class InvTestKit {
    static final String P = "[vibeyclient-inventory-test] ";
    static final ReviewShots SHOTS = new ReviewShots("inventory");

    private InvTestKit() {}

    static InventoryModule m() {
        return ModuleManager.get().get(InventoryModule.class);
    }

    static void cmd(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(args.length == 0 ? format : String.format(Locale.ROOT, format, args));
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static void log(String format, Object... args) {
        System.out.println(P + String.format(Locale.ROOT, format, args));
    }

    static void shot(ClientGameTestContext ctx, String name) {
        SHOTS.shot(ctx, name);
    }

    static void hoverSlot(ClientGameTestContext ctx, int slot) {
        double[] p = ctx.computeOnClient(mc -> {
            AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) mc.screen;
            ContainerScreenAccess a = (ContainerScreenAccess) s;
            Slot sl = s.getMenu().getSlot(slot);
            double gs = mc.getWindow().getGuiScale();
            return new double[] {(a.vibey$leftPos() + sl.x + 8) * gs, (a.vibey$topPos() + sl.y + 8) * gs};
        });
        ctx.getInput().setCursorPos(p[0], p[1]);
        ctx.waitTicks(3);
    }

    static int count(ClientGameTestContext ctx, int slot) {
        return ctx.computeOnClient(mc -> mc.player.containerMenu.getSlot(slot).getItem().getCount());
    }

    static String item(ClientGameTestContext ctx, int invSlot) {
        return ctx.computeOnClient(mc -> mc.player.getInventory().getItem(invSlot).getItem().toString());
    }
}

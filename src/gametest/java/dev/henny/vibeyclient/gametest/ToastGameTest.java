package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.toast.ToastStack;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * The Vibey toast stack and vanilla toasts share the top right corner: ours goes below the visible vanilla toasts
 * ("Chat messages can't be verified", advancements, recipes) and moves back up when they are gone. Screenshots
 * {@code toast_1_before_overlap} (old behaviour, layout switched off), {@code toast_2_below_vanilla},
 * {@code toast_3_vanilla_gone}.
 */
public final class ToastGameTest implements FabricClientGameTest {
    private static final String LOG = "[vibeyclient-toast-test] ";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeytoasts")).create()) {
            world.getServer().runCommand("time set 1000");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule announceAdvancements false");
            world.getClientWorld().waitForChunksRender();
            ctx.runOnClient(mc -> {
                mc.gui.getChat().clearMessages(false);
                mc.options.hideGui = false;
                mc.player.setXRot(-8f);
            });
            ctx.waitTicks(10);
            if (ctx.computeOnClient(mc -> ClientCompat.vanillaToastBottom()) > 0f) {
                ctx.runOnClient(mc -> mc.getToastManager().clear());
                ctx.waitTicks(5);
            }

            // two vanilla toasts (the real "chat can't be verified" text) + two Vibey toasts
            ctx.runOnClient(mc -> {
                mc.getToastManager().addToast(SystemToast.multiline(mc, SystemToast.SystemToastId.UNSECURE_SERVER_WARNING,
                        Component.translatable("multiplayer.unsecureserver.toast.title"),
                        Component.translatable("multiplayer.unsecureserver.toast")));
                SystemToast.add(mc.getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                        Component.literal("Advancement Made!"), Component.literal("Stone Age"));
                Notify.show(new Notify.Toast("Friend online", "Bee is playing now", Notify.Kind.SOCIAL, "B", null));
                Notify.show(new Notify.Toast("Invite link copied", "http://127.0.0.1:3470/j/k7m2xq", Notify.Kind.SUCCESS, null, null));
            });
            ctx.waitTicks(30); // vanilla slides in for 600 ms, ours moves within 150 ms

            float bottom = ctx.computeOnClient(mc -> ClientCompat.vanillaToastBottom());
            if (bottom < 64f) throw new AssertionError("vanilla toasts not seen: bottom " + bottom);

            // what it looked like before: the stack on top of the vanilla toasts
            ToastStack.belowVanilla = false;
            ctx.waitTicks(10);
            ctx.takeScreenshot("toast_1_before_overlap");
            ToastStack.belowVanilla = true;
            ctx.waitTicks(10);
            int top = ctx.computeOnClient(mc -> ToastStack.debugTop());
            if (top < bottom) throw new AssertionError("Vibey toasts at y " + top + " overlap vanilla toasts down to " + bottom);
            ctx.takeScreenshot("toast_2_below_vanilla");
            log("vanilla bottom " + bottom + ", Vibey stack from y " + top);

            // vanilla toasts go (system toasts last 5 s, the multiline one a bit longer): the stack moves back up
            ctx.runOnClient(mc -> {
                SystemToast.forceHide(mc.getToastManager(), SystemToast.SystemToastId.UNSECURE_SERVER_WARNING);
                SystemToast.forceHide(mc.getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION);
            });
            boolean up = false;
            for (int t = 0; t < 60 && !up; t += 2) {
                ctx.waitTicks(2);
                up = ctx.computeOnClient(mc -> ToastStack.debugTop()) <= 6 && ctx.computeOnClient(mc -> ClientCompat.vanillaToastBottom()) <= 0.5f;
            }
            if (!up) throw new AssertionError("stack did not move up: top " + ctx.computeOnClient(mc -> ToastStack.debugTop()));
            ctx.waitTicks(6);
            ctx.takeScreenshot("toast_3_vanilla_gone");
            log("vanilla gone, stack back at the top");
            ctx.runOnClient(mc -> mc.getToastManager().clear());
            ctx.waitTicks(20 * 6); // let ours expire before the next test
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
        log("done");
    }

    private static void log(String s) {
        System.out.println(LOG + s);
    }
}

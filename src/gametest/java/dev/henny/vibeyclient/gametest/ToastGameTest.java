package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.vui.toast.VuiToasts;
import dev.henny.vui.dom.Element;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ToastGameTest implements FabricClientGameTest {
    private static final String LOG = "[vibeyclient-toast-test] ";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        UiTest t = new UiTest(ctx, "ui-new");
        t.window(1280, 720);
        t.scale(3);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        AtomicBoolean acted = new AtomicBoolean();
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeytoasts")).create()) {
            world.getServer().runCommand("time set 1000");
            world.getServer().runCommand("gamerule announceAdvancements false");
            world.getClientWorld().waitForChunksRender();
            ctx.runOnClient(mc -> {
                mc.getToastManager().clear();
                mc.getToastManager().addToast(SystemToast.multiline(mc, SystemToast.SystemToastId.UNSECURE_SERVER_WARNING,
                        Component.translatable("multiplayer.unsecureserver.toast.title"),
                        Component.translatable("multiplayer.unsecureserver.toast")));
                Notify.show(new Notify.Toast("Bee", "Online", Notify.Kind.SOCIAL, null, null));
                Notify.show(new Notify.Toast("New cosmetics", "3 items", Notify.Kind.INFO, null, () -> acted.set(true)));
            });
            ctx.waitTicks(30);
            UiTest.check(count(ctx) == 2, "expected 2 Vibey toasts, got " + count(ctx));
            float vanilla = ctx.computeOnClient(mc -> ClientCompat.vanillaToastBottom());
            float top = ctx.computeOnClient(mc -> boxes().get(0)[1]);
            UiTest.check(vanilla > 0, "vanilla toast not seen");
            UiTest.check(top > vanilla, "Vibey toasts (y " + top + ") overlap vanilla toasts down to " + vanilla);
            t.shot("toast_1_bottom_right");
            log("vanilla bottom " + vanilla + ", Vibey stack from y " + top);

            ctx.setScreen(() -> new PauseScreen(true));
            ctx.waitTicks(4);
            float[] action = ctx.computeOnClient(mc -> actionBox());
            UiTest.check(action != null, "no action button on the toast");
            t.hover(action[0] - 30, action[1] + action[3] / 2);
            ctx.waitTicks(4);
            UiTest.check(ctx.computeOnClient(mc -> VuiToasts.stack().paused()), "hovering a toast with a button should pause it");
            t.shot("toast_2_hover");
            t.click(action[0] + action[2] / 2, action[1] + action[3] / 2);
            ctx.waitTicks(8);
            UiTest.check(acted.get(), "the toast action did not run");
            UiTest.check(count(ctx) == 1, "the toast with the action should be gone, " + count(ctx) + " left");
            float[] rest = ctx.computeOnClient(mc -> boxes().get(0));
            boolean taken = ctx.computeOnClient(mc -> VuiToasts.click(rest[0] + 20, rest[1] + rest[3] / 2, 0));
            ctx.waitTicks(2);
            UiTest.check(!taken && count(ctx) == 1, "a toast without a button must let clicks through (taken " + taken + ")");
            t.shot("toast_3_click_through");
            ctx.setScreen(() -> null);
            ctx.runOnClient(mc -> mc.getToastManager().clear());
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
        log("done");
    }

    private static int count(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> VuiToasts.stack().toasts().size());
    }

    private static List<float[]> boxes() {
        var doc = VuiToasts.stack().document();
        doc.layoutIfNeeded();
        return doc.query("toast").stream().map(e -> new float[] {e.box().x, e.box().y, e.box().width, e.box().height}).toList();
    }

    private static float[] actionBox() {
        var doc = VuiToasts.stack().document();
        doc.layoutIfNeeded();
        List<Element> list = doc.query(".toast-action");
        if (list.isEmpty()) return null;
        var b = list.get(0).box();
        return new float[] {b.x, b.y, b.width, b.height};
    }

    private static void log(String s) {
        System.out.println(LOG + s);
    }
}

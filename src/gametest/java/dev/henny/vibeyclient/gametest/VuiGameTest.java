package dev.henny.vibeyclient.gametest;

import dev.henny.vui.demo.DemoController;
import dev.henny.vui.dom.Document;
import dev.henny.vui.hud.HudWidget;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.VuiRuntime;
import dev.henny.vui.mc.dev.DevTools;
import dev.henny.vui.mc.host.HudEditScreen;
import dev.henny.vui.mc.host.VuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;

public final class VuiGameTest implements FabricClientGameTest {
    private static final int ESCAPE = 256;

    @Override
    public void runTest(ClientGameTestContext context) {
        VuiTestSupport t = new VuiTestSupport(context);
        context.getInput().resizeWindow(1280, 720);
        t.guiScale(2);
        DemoController demo = context.computeOnClient(mc -> (DemoController) VuiMc.dev().createController(DevTools.DEMO));
        openDemo(context, demo);
        t.assertNoErrors();
        t.shot("vui_01_demo");

        toggle(context, t, demo);
        textField(context, t, demo);
        select(context, t, demo);
        inspector(context, t);
        hudEditor(context, t);

        openDemo(context, demo);
        VuiHotReloadCheck.run(context, t);
        context.setScreen(() -> null);
        context.waitTicks(2);
    }

    private static void openDemo(ClientGameTestContext context, DemoController demo) {
        context.setScreen(() -> VuiMc.screen(DevTools.DEMO, demo).pause(false));
        context.waitTicks(10);
    }

    private static void toggle(ClientGameTestContext context, VuiTestSupport t, DemoController demo) {
        boolean before = demo.sound.peek();
        t.click("sound");
        context.waitTicks(10);
        if (demo.sound.peek() == before) throw new AssertionError("toggle did not flip 'sound'");
        t.shot("vui_02_toggle");
    }

    private static void textField(ClientGameTestContext context, VuiTestSupport t, DemoController demo) {
        t.click("name");
        context.getInput().typeChars("Steve");
        context.waitTicks(4);
        if (!"Steve".equals(demo.name.peek())) throw new AssertionError("textfield wrote '" + demo.name.peek() + "'");
        t.shot("vui_03_textfield");
    }

    private static void select(ClientGameTestContext context, VuiTestSupport t, DemoController demo) {
        t.click("mode");
        context.waitTicks(6);
        int open = context.computeOnClient(mc -> document(mc).overlays().open().size());
        if (open == 0) throw new AssertionError("select menu did not open");
        t.shot("vui_04_select_open");
        t.click(t.centerOf("select-option", 2));
        context.waitTicks(6);
        if (!"never".equals(demo.mode.peek())) throw new AssertionError("select chose '" + demo.mode.peek() + "'");
        t.shot("vui_05_select_chosen");
    }

    private static void inspector(ClientGameTestContext context, VuiTestSupport t) {
        context.runOnClient(mc -> VuiRuntime.get().inspector().set(true));
        t.pointAt(t.center("save"));
        context.waitTicks(4);
        t.shot("vui_06_inspector");
        context.runOnClient(mc -> VuiRuntime.get().inspector().set(false));
    }

    private static void hudEditor(ClientGameTestContext context, VuiTestSupport t) {
        context.runOnClient(mc -> {
            VuiMc.dev().demoHud();
            VuiMc.hud().openEditor();
        });
        context.waitTicks(6);
        boolean active = context.computeOnClient(mc -> mc.screen instanceof HudEditScreen s && s.editor().active());
        if (!active) throw new AssertionError("HUD editor not active");
        float[] fps = context.computeOnClient(mc -> {
            HudWidget w = VuiMc.hud().layer().widget("vui:fps");
            return new float[]{w.bounds().centerX(), w.bounds().centerY()};
        });
        t.pointAt(fps);
        context.getInput().holdMouse(0);
        for (int i = 1; i <= 8; i++) t.pointAt(new float[]{fps[0] + i * 12, fps[1] + i * 6});
        context.getInput().releaseMouse(0);
        context.waitTicks(2);
        float moved = context.computeOnClient(mc -> VuiMc.hud().layer().widget("vui:fps").bounds().centerX());
        if (moved <= fps[0] + 20) throw new AssertionError("HUD widget was not dragged: " + fps[0] + " -> " + moved);
        t.shot("vui_07_hud_editor");
        context.getInput().pressKey(ESCAPE);
        context.waitTicks(3);
        boolean closed = context.computeOnClient(mc -> !(mc.screen instanceof HudEditScreen) && !VuiMc.hud().editor().active());
        if (!closed) throw new AssertionError("Esc did not close the HUD editor");
    }

    static Document document(Minecraft mc) {
        return mc.screen instanceof VuiScreen s ? s.document() : null;
    }
}

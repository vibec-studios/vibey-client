package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.hud.HudModule;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudTemplates;
import dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.List;

public final class HudGameTest implements FabricClientGameTest {
    static final List<String> BUILTIN = List.of("hud.fps", "hud.cps", "hud.keystrokes", "hud.coords", "hud.armor", "hud.effects",
            "hud.clock", "hud.ping", "hud.server", "hud.speed", "hud.time", "hud.items", "hud.memory", "hud.compass",
            "hud.timer", "hud.scoreboard", "hud.tablist", "hud.bossbar", "hud.title");

    @Override
    public void runTest(ClientGameTestContext context) {
        HudTestKit t = new HudTestKit(context);
        context.getInput().resizeWindow(1280, 720);
        t.guiScale(2);
        context.runOnClient(mc -> {
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            if (!ModuleManager.get().isEnabled("hud")) throw new AssertionError("hud module off");
            HudLayout.setFlag("intro", false);
            for (String k : BUILTIN) if (HudLayout.get(k) == null) throw new AssertionError("widget missing: " + k);
            if (!HudModule.instance().defaultsApplied()) throw new AssertionError("default template not applied");
            HudTemplates.apply(HudModule.DEFAULT_TEMPLATE);
        });
        HudShareChecks.run(t);
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeyhud")).create()) {
            HudWorld.prepare(t, world);
            t.shot("hud_01_default_ingame");
            HudEditorChecks.run(t);
            HudNewWidgetChecks.run(t, world);
            HudServerChecks.run(t);
            HudVideo.run(t);
        }
        context.runOnClient(mc -> {
            ServerRules.clear();
            HudEditorScreen.drawPointer = false;
            HudLayout.resetAll();
            HudTemplates.apply("minimal");
            HudLayout.save();
        });
    }
}

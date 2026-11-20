package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudTemplates;

import java.util.List;

import static dev.henny.vibeyclient.gametest.HudTestKit.check;

final class HudServerChecks {
    private HudServerChecks() {}

    static void run(HudTestKit t) {
        t.context.runOnClient(mc -> {
            HudTemplates.apply("building");
            ServerRules.apply(List.of("hud.coords"), "No coordinates in this event");
        });
        check(t.context.computeOnClient(mc -> HudLayout.serverDisabled(HudLayout.get("hud.coords"))), "coords not disabled");
        check(!t.context.computeOnClient(mc -> HudLayout.serverDisabled(HudLayout.get("hud.fps"))), "fps disabled by mistake");
        t.context.waitTicks(4);
        t.shot("hud_30_server_disabled_ingame");
        t.openEditor();
        t.context.waitTicks(4);
        t.shot("hud_30_server_disabled_editor");
        t.click("dock-widgets");
        t.context.waitTicks(6);
        t.shot("hud_30_server_disabled_library");
        t.context.setScreen(() -> null);
        t.context.runOnClient(mc -> ServerRules.clear());
        t.context.waitTicks(2);
    }
}

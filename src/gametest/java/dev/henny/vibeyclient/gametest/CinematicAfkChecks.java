package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.cinematic.CinematicRules;
import dev.henny.vibeyclient.module.cinematic.afk.AfkEditor;
import dev.henny.vibeyclient.module.cinematic.afk.AfkLayout;
import dev.henny.vibeyclient.module.cinematic.core.plan.Mode;
import dev.henny.vibeyclient.module.cinematic.run.AfkWatch;
import dev.henny.vibeyclient.module.cinematic.run.Cinematic;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudProfile;
import dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static dev.henny.vibeyclient.gametest.CinematicChecks.m;
import static dev.henny.vibeyclient.gametest.CinematicGameTest.KIT;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;

final class CinematicAfkChecks {
    private CinematicAfkChecks() {}

    static void afkScreen(ClientGameTestContext ctx, String preset, String name) {
        ctx.runOnClient(mc -> {
            m().mode.set(Mode.BUILDS);
            check(Cinematic.start(false), "start for the AFK screen");
            check(AfkLayout.showing(), "AFK layout should be active");
            dev.henny.vibeyclient.ui.hud.HudTemplates.apply(preset);
        });
        ctx.waitTicks(90);
        KIT.shot(ctx, name);
        ctx.runOnClient(mc -> Cinematic.abort());
        ctx.waitTicks(3);
        ctx.runOnClient(mc -> check(HudLayout.profile().equals(HudProfile.MAIN), "main HUD back after the cinematic"));
    }

    static void editor(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> AfkEditor.open(null));
        ctx.waitTicks(40);
        ctx.runOnClient(mc -> {
            check(mc.screen instanceof HudEditorScreen, "AFK editor open");
            check(AfkLayout.showing(), "editor edits the AFK layout");
            check(Cinematic.preview(), "camera preview runs behind the editor");
            ((HudEditorScreen) mc.screen).select(AfkLayout.TITLE);
        });
        ctx.waitTicks(10);
        KIT.shot(ctx, "afk_editor");
        ctx.runOnClient(mc -> {
            var e = HudLayout.get(AfkLayout.TITLE);
            HudLayout.scaleTo(e, 2.5f, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            ((HudEditorScreen) mc.screen).refresh();
        });
        ctx.waitTicks(10);
        KIT.shot(ctx, "afk_editor_scaled");
        ctx.setScreen(() -> null);
        ctx.waitTicks(5);
        ctx.runOnClient(mc -> {
            check(!Cinematic.active(), "preview ends with the editor");
            check(HudLayout.profile().equals(HudProfile.MAIN), "main HUD back after the editor");
            check(HudLayout.get(AfkLayout.TITLE) != null, "title widget registered");
        });
    }

    static void returnToPlayer(ClientGameTestContext ctx) {
        double[] before = CinematicChecks.player(ctx);
        ctx.runOnClient(mc -> {
            m().mode.set(Mode.SELF);
            Cinematic.start(false);
        });
        ctx.waitTicks(90);
        KIT.shot(ctx, "return_before");
        ctx.getInput().setCursorPos(200, 200);
        ctx.waitTicks(2);
        ctx.getInput().setCursorPos(320, 260);
        ctx.waitTicks(8);
        KIT.shotNow(ctx, "return_flying");
        ctx.waitTicks(40);
        ctx.runOnClient(mc -> {
            check(!Cinematic.active(), "mouse move ends the cinematic");
            Vec3 cam = mc.gameRenderer.getMainCamera().position();
            check(cam.distanceTo(mc.player.getEyePosition()) < 0.05, "camera back at the eyes: " + cam);
            check(HudLayout.profile().equals(HudProfile.MAIN), "main HUD back");
        });
        KIT.shot(ctx, "return_done");
        CinematicChecks.samePlayer(before, CinematicChecks.player(ctx), "return");
    }

    static void triggers(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            m().auto.set(true);
            m().afkMinutes.set(1);
            AfkWatch.backdate(61_000);
        });
        ctx.waitTicks(5);
        ctx.runOnClient(mc -> check(Cinematic.active(), "auto start after the AFK time"));
        ctx.getInput().pressKey(Keys.SPACE);
        ctx.waitTicks(40);
        ctx.runOnClient(mc -> check(!Cinematic.active(), "a key press ends it"));
        ctx.getInput().pressKey(Keys.F6);
        ctx.waitTicks(5);
        ctx.runOnClient(mc -> check(Cinematic.active(), "start key"));
        ctx.runOnClient(mc -> ServerRules.apply(List.of(CinematicRules.AFK), "No AFK cams"));
        ctx.waitTicks(3);
        ctx.runOnClient(mc -> {
            check(!Cinematic.active(), "server rule stops it at once");
            check(!Cinematic.start(false), "server rule blocks a new start");
            AfkWatch.backdate(120_000);
        });
        ctx.waitTicks(5);
        ctx.runOnClient(mc -> check(!Cinematic.active(), "no auto start while the server forbids it"));
        ctx.runOnClient(mc -> ServerRules.clear());
        KIT.log("triggers: auto, key, input, server rule ok");
    }
}

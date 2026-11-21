package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings;
import dev.henny.vibeyclient.client.ServerPresentationClient;
import dev.henny.vibeyclient.client.ServerRulesClient;
import dev.henny.vibeyclient.net.RulesPayloads;
import dev.henny.vibeyclient.net.ServerPresentation;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.renderer.LevelTargetBundle;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static dev.henny.vibeyclient.gametest.ReviewKit.check;

public final class ServerApiAccessibilityGameTest implements FabricClientGameTest {
    private static final ReviewKit KIT = new ReviewKit("server-api-accessibility");

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
        ModulesPanel.preselect("accessibility");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        KIT.shot(ctx, "sehhilfen");
        ctx.setScreen(() -> null);

        try (var world = ctx.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            ctx.runOnClient(mc -> {
                mc.options.showSubtitles().set(true);
                var settings = ModuleManager.get().get("accessibility").settings();
                ((Settings.BoolSetting) settings.get("hud")).set(true);
                choose(settings.get("subtitles"), "YELLOW");

                var pos = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(12));
                var player = mc.player.getUUID();
                var waypoint = new ServerPresentation.Waypoint("spawn", "Treffpunkt",
                        mc.level.dimension().identifier().toString(), (int) pos.x, (int) pos.y, (int) pos.z, 0x33BBFF);
                var team = new ServerPresentation.Team("blue", "Blau", 0x33BBFF, List.of(player));
                var snapshot = new ServerPresentation(List.of(waypoint), List.of(team), Map.of());
                ServerRulesClient.handle(new RulesPayloads.Presentation(snapshot.encode()));
                check(ServerPresentationClient.waypoints().equals(List.of(waypoint)), "server waypoint was not received");
                check(Integer.valueOf(0x33BBFF).equals(ServerPresentationClient.nametagColor(player)), "team colour was not received");
            });

            for (String mode : List.of("PROTANOPIA", "DEUTERANOPIA", "TRITANOPIA")) {
                ctx.runOnClient(mc -> {
                    var id = Compat.id("accessibility_" + mode.toLowerCase(Locale.ROOT));
                    check(mc.getShaderManager().getPostChain(id, LevelTargetBundle.MAIN_TARGETS) != null,
                            "missing colour filter " + mode);
                    choose(ModuleManager.get().get("accessibility").settings().get("filter"), mode);
                });
                ctx.waitTicks(10);
                KIT.shot(ctx, mode.toLowerCase(Locale.ROOT));
            }
        }

        ctx.runOnClient(mc -> {
            check(ServerPresentationClient.waypoints().isEmpty(), "server waypoints survived disconnect");
            ModuleManager.get().get("accessibility").settings().all().forEach(setting -> setting.reset());
            GlobalSettings.language.set(GlobalSettings.Language.AUTO);
        });
    }

    private static void choose(Setting<?, ?> setting, String name) {
        choose((Settings.EnumSetting<?>) setting, name);
    }

    private static <E extends Enum<E>> void choose(Settings.EnumSetting<E> setting, String name) {
        for (E value : setting.values()) {
            if (value.name().equals(name)) {
                setting.set(value);
                return;
            }
        }
        throw new AssertionError("unknown value " + name + " for " + setting.key());
    }
}

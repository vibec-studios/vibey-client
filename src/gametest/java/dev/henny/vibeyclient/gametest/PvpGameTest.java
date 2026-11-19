package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.pvp.Crosshair;
import dev.henny.vibeyclient.module.pvp.HitColor;
import dev.henny.vibeyclient.module.pvp.PvpModule;
import dev.henny.vibeyclient.module.pvp.ToggleMovement;
import dev.henny.vibeyclient.module.pvp.Zoom;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The pvp module in the real game (Fabric 1.21.11): module page (en/de), crosshair editor and HUD crosshair styles,
 * zoom levels (key, smooth ease, mouse wheel; FOV and mouse speed restored), block outline styles, hit colour on an
 * iron golem, low fire, lower shield (idle and blocking), toggle sprint with its HUD indicator, and a feature disabled
 * by the server (zoom key does nothing, the page shows the badge). Screenshots:
 * versions/fabric-1.21.11/build/run/clientGameTest/screenshots/NNNN_pvp_*.png (tools/pvp-review.py crops them).
 */
public final class PvpGameTest implements FabricClientGameTest {
    private static final String P = "[vibeyclient-pvp-test] ";

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        guiScale(context, 2);
        context.runOnClient(mc -> {
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            PvpModule m = m();
            if (m == null || !m.isEnabled()) throw new AssertionError("pvp module should be on");
            for (String key : new String[] {"zoom", "togglesprint", "togglesneak", "crosshair", "blockoutline", "hitcolor",
                    "lowfire", "lowshield", "hud.togglestate"}) {
                if (!ServerRules.supportedKeys().contains(key)) throw new AssertionError("rule key not declared: " + key);
            }
            m.settings().all().forEach(s -> s.reset());
        });

        // ---------------------------------------------------------------------------------------- module page
        ModulesPanel.preselect("pvp");
        context.setScreen(() -> new VibeyScreen(null));
        context.waitTicks(10);
        shot(context, "pvp_01_page_en");
        context.runOnClient(mc -> {
            m().toggleSprint.set(true);
            m().crosshair.set(Crosshair.Shape.CROSS);
            m().outline.set(true);
        });
        context.waitTicks(4);
        scrollMiddle(context, -6);
        shot(context, "pvp_02_page_en_scrolled");
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
        ModulesPanel.preselect("pvp");
        context.setScreen(() -> new VibeyScreen(null));
        context.waitTicks(10);
        shot(context, "pvp_03_page_de");
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        context.setScreen(() -> null);
        context.runOnClient(mc -> m().editCrosshair.run());
        context.waitTicks(6);
        shot(context, "pvp_04_editor_title");
        context.setScreen(() -> null);
        context.runOnClient(mc -> m().settings().all().forEach(s -> s.reset()));

        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeypvp"))
                .create()) {
            world.getClientWorld().waitForChunksRender();
            int[] p = context.computeOnClient(mc -> new int[] {(int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getZ())});
            int x = p[0], z = p[1], y = 150;
            cmd(world, "gamemode creative @a");
            cmd(world, "time set 6000");
            cmd(world, "weather clear");
            cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 14, y + 1, z - 20, x + 14, y + 12, z + 14);
            cmd(world, "fill %d %d %d %d %d %d minecraft:grass_block", x - 14, y, z - 20, x + 14, y, z + 14);
            // a target wall 16 blocks north: white board, red ring, black centre, some coloured marks
            cmd(world, "fill %d %d %d %d %d %d minecraft:white_concrete", x - 4, y + 1, z - 16, x + 4, y + 9, z - 16);
            cmd(world, "fill %d %d %d %d %d %d minecraft:red_concrete", x - 2, y + 3, z - 16, x + 2, y + 7, z - 16);
            cmd(world, "fill %d %d %d %d %d %d minecraft:white_concrete", x - 1, y + 4, z - 16, x + 1, y + 6, z - 16);
            cmd(world, "setblock %d %d %d minecraft:black_concrete", x, y + 5, z - 16);
            cmd(world, "setblock %d %d %d minecraft:blue_concrete", x - 4, y + 9, z - 16);
            cmd(world, "setblock %d %d %d minecraft:lime_concrete", x + 4, y + 9, z - 16);
            cmd(world, "tp @a %.1f %d %.1f 180 0", x + 0.5, y + 1, z + 0.5);
            context.waitTicks(40);
            world.getClientWorld().waitForChunksRender();
            look(context, 180, -9);
            context.waitTicks(5);

            crosshairs(context, world);
            zoom(context, world, x, y, z);
            outline(context, world, x, y, z);
            hitColor(context, world, x, y, z);
            lowFire(context, world, x, y, z);
            shield(context, world, x, y, z);
            toggleSprint(context, world, x, y, z);
            serverRule(context);

            context.setScreen(() -> null);
            context.runOnClient(mc -> {
                mc.options.hideGui = false;
                m().settings().all().forEach(s -> s.reset());
            });
        }
        guiScale(context, 0);
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
    }

    // ============================================================================================ crosshair

    private static void crosshairs(ClientGameTestContext ctx, TestSingleplayerContext world) {
        shot(ctx, "pvp_10_crosshair_vanilla");
        style(ctx, "pvp_11_crosshair_cross", m -> {
            m.crosshair.set(Crosshair.Shape.CROSS);
        });
        style(ctx, "pvp_12_crosshair_cross_green_thick_dot", m -> {
            m.crosshair.set(Crosshair.Shape.CROSS);
            m.chColor.set(0xFF3BFF6B);
            m.chThickness.set(2);
            m.chSize.set(6);
            m.chGap.set(3);
            m.chDot.set(true);
        });
        style(ctx, "pvp_13_crosshair_circle_cyan", m -> {
            m.crosshair.set(Crosshair.Shape.CIRCLE);
            m.chColor.set(0xFF3FE0C5);
            m.chSize.set(4);
            m.chGap.set(1);
            m.chDot.set(true);
        });
        style(ctx, "pvp_14_crosshair_t_red", m -> {
            m.crosshair.set(Crosshair.Shape.T);
            m.chColor.set(0xFFFF3B3B);
            m.chSize.set(4);
            m.chGap.set(1);
        });
        style(ctx, "pvp_15_crosshair_square_yellow", m -> {
            m.crosshair.set(Crosshair.Shape.SQUARE);
            m.chColor.set(0xFFFFE14D);
            m.chSize.set(3);
            m.chGap.set(1);
            m.chDot.set(true);
        });
        style(ctx, "pvp_16_crosshair_dot_pink", m -> {
            m.crosshair.set(Crosshair.Shape.DOT);
            m.chColor.set(0xFFFF5CE1);
            m.chSize.set(5);
        });
        style(ctx, "pvp_17_crosshair_invert", m -> {
            m.crosshair.set(Crosshair.Shape.CROSS);
            m.chInvert.set(true);
            m.chSize.set(6);
        });
        // the editor over the world (the real HUD crosshair stays live in the middle)
        ctx.runOnClient(mc -> {
            reset(m());
            m().crosshair.set(Crosshair.Shape.CIRCLE);
            m().chColor.set(0xFF8B6CFF);
            m().chDot.set(true);
            m().editCrosshair.run();
        });
        ctx.waitTicks(6);
        shot(ctx, "pvp_18_editor_world_circle");
        ctx.runOnClient(mc -> {
            m().crosshair.set(Crosshair.Shape.CROSS);
            m().chColor.set(0xFF3BFF6B);
            m().chThickness.set(2);
        });
        ctx.waitTicks(3);
        shot(ctx, "pvp_19_editor_world_cross");
        ctx.setScreen(() -> null);
        // vanilla rules: F1 hides it, spectator (looking at nothing) has none
        ctx.runOnClient(mc -> mc.options.hideGui = true);
        ctx.waitTicks(3);
        shot(ctx, "pvp_20_crosshair_f1_hidden");
        ctx.runOnClient(mc -> mc.options.hideGui = false);
        cmd(world, "gamemode spectator @a");
        ctx.waitTicks(5);
        shot(ctx, "pvp_21_crosshair_spectator_hidden");
        cmd(world, "gamemode creative @a");
        ctx.waitTicks(5);
        ctx.runOnClient(mc -> reset(m()));
    }

    private static void style(ClientGameTestContext ctx, String shot, Consumer<PvpModule> setup) {
        ctx.runOnClient(mc -> {
            reset(m());
            setup.accept(m());
        });
        ctx.waitTicks(3);
        shot(ctx, shot);
    }

    // ============================================================================================ zoom

    private static void zoom(ClientGameTestContext ctx, TestSingleplayerContext world, int x, int y, int z) {
        look(ctx, 180, -9);
        double fov0 = ctx.computeOnClient(mc -> (double) mc.options.fov().get());
        double sens0 = ctx.computeOnClient(mc -> mc.options.sensitivity().get());
        int slot0 = ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
        shot(ctx, "pvp_30_zoom_off");
        ctx.getInput().holdKey(m().zoomKey.mapping());
        double f = settle(ctx, 4);
        System.out.println(P + "zoom held: factor " + f);
        if (Math.abs(f - 4) > 0.05) throw new AssertionError("zoom should reach 4x, got " + f);
        shot(ctx, "pvp_31_zoom_4x");
        ctx.getInput().scroll(1);
        ctx.waitTicks(1);
        ctx.getInput().scroll(1);
        ctx.waitTicks(1);
        ctx.getInput().scroll(1);
        double want = 4 * Math.pow(1.25, 3);
        f = settle(ctx, want);
        int slot1 = ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
        System.out.println(P + "zoom scrolled: factor " + f + " slot " + slot0 + " -> " + slot1);
        if (Math.abs(f - want) > 0.08) throw new AssertionError("wheel should zoom to " + want + ", got " + f);
        if (slot0 != slot1) throw new AssertionError("the wheel moved the hotbar while zooming");
        shot(ctx, "pvp_32_zoom_scrolled_7_8x");
        // halfway out (smooth ease): a frame or two after letting go
        ctx.getInput().releaseKey(m().zoomKey.mapping());
        f = settle(ctx, 1);
        if (f != 1) throw new AssertionError("zoom should end at exactly 1, got " + f);
        double fov1 = ctx.computeOnClient(mc -> (double) mc.options.fov().get());
        double sens1 = ctx.computeOnClient(mc -> mc.options.sensitivity().get());
        if (fov0 != fov1 || sens0 != sens1) throw new AssertionError("options changed: fov " + fov0 + "→" + fov1 + " sens " + sens0 + "→" + sens1);
        double mouse = ctx.computeOnClient(mc -> Zoom.mouseScale());
        if (mouse != 1) throw new AssertionError("mouse scale not restored: " + mouse);
        // the next hold starts at the configured level again, and 10x without the wheel
        ctx.runOnClient(mc -> m().zoomLevel.set(10f));
        ctx.getInput().holdKey(m().zoomKey.mapping());
        f = settle(ctx, 10);
        shot(ctx, "pvp_33_zoom_10x");
        ctx.getInput().releaseKey(m().zoomKey.mapping());
        settle(ctx, 1);
        ctx.runOnClient(mc -> m().zoomLevel.reset());
        shot(ctx, "pvp_34_zoom_released");
    }

    /** Waits until the eased zoom factor is at {@code target} (max ~3 s), returns it. */
    private static double settle(ClientGameTestContext ctx, double target) {
        double f = 0;
        for (int i = 0; i < 60; i++) {
            ctx.waitTicks(1);
            f = ctx.computeOnClient(mc -> Zoom.factor());
            if (Math.abs(f - target) < 0.01 * target) {
                ctx.waitTicks(2);
                return ctx.computeOnClient(mc -> Zoom.factor());
            }
        }
        return f;
    }

    // ============================================================================================ block outline

    private static void outline(ClientGameTestContext ctx, TestSingleplayerContext world, int x, int y, int z) {
        cmd(world, "setblock %d %d %d minecraft:stone", x, y + 1, z - 2);
        cmd(world, "setblock %d %d %d minecraft:oak_fence", x + 1, y + 1, z - 2);
        cmd(world, "tp @a %.1f %d %.1f 180 30", x + 0.5, y + 1, z + 0.5);
        ctx.waitTicks(5);
        lookAt(ctx, x + 0.5, y + 1.5, z - 1.5);
        ctx.waitTicks(3);
        shot(ctx, "pvp_40_outline_vanilla");
        outlineStyle(ctx, "pvp_41_outline_brand", m -> m.outline.set(true));
        outlineStyle(ctx, "pvp_42_outline_white_thick", m -> {
            m.outline.set(true);
            m.outlineColor.set(0xFFFFFFFF);
            m.outlineWidth.set(5f);
        });
        outlineStyle(ctx, "pvp_43_outline_cyan_fill", m -> {
            m.outline.set(true);
            m.outlineColor.set(0xFF3FE0C5);
            m.outlineFill.set(true);
            m.outlineFillOpacity.set(30);
        });
        cmd(world, "tp @a %.1f %d %.1f 180 25", x + 1 + 0.5, y + 1, z + 0.5); // the fence: a non-cube shape
        ctx.waitTicks(5);
        lookAt(ctx, x + 1.5, y + 1.6, z - 1.5);
        outlineStyle(ctx, "pvp_44_outline_fence_red_fill", m -> {
            m.outline.set(true);
            m.outlineColor.set(0xFFFF3B3B);
            m.outlineFill.set(true);
        });
        ctx.runOnClient(mc -> reset(m()));
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x, y + 1, z - 2, x + 1, y + 1, z - 2);
        cmd(world, "tp @a %.1f %d %.1f 180 0", x + 0.5, y + 1, z + 0.5);
        ctx.waitTicks(5);
    }

    private static void outlineStyle(ClientGameTestContext ctx, String shot, Consumer<PvpModule> setup) {
        ctx.runOnClient(mc -> {
            reset(m());
            setup.accept(m());
        });
        ctx.waitTicks(3);
        boolean hit = ctx.computeOnClient(mc -> mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK);
        if (!hit) throw new AssertionError("not looking at a block for " + shot);
        shot(ctx, shot);
    }

    // ============================================================================================ hit colour

    private static void hitColor(ClientGameTestContext ctx, TestSingleplayerContext world, int x, int y, int z) {
        cmd(world, "summon minecraft:iron_golem %.1f %d %.1f {NoAI:1b,Rotation:[0f,0f],Health:100f}", x + 0.5, y + 1, z - 4 + 0.5);
        ctx.waitTicks(10);
        look(ctx, 180, 4);
        ctx.waitTicks(3);
        shot(ctx, "pvp_50_golem_unhurt");
        hurtShot(ctx, world, "pvp_51_hit_vanilla_red", m -> {});
        hurtShot(ctx, world, "pvp_52_hit_blue", m -> {
            m.hitColor.set(true);
            m.hitTint.set(0xFF4D7BFF);
        });
        hurtShot(ctx, world, "pvp_53_hit_lime_strong", m -> {
            m.hitColor.set(true);
            m.hitTint.set(0xFF3BFF6B);
            m.hitStrength.set(60);
        });
        int painted = ctx.computeOnClient(mc -> HitColor.painted());
        if (painted != ((Math.round(40 * 2.55f) << 24) | 0x3BFF6B)) throw new AssertionError(String.format("hit tint not painted: %08X", painted));
        ctx.runOnClient(mc -> reset(m()));
        ctx.waitTicks(2);
        painted = ctx.computeOnClient(mc -> HitColor.painted());
        if (painted != 0xB3FF0000) throw new AssertionError(String.format("vanilla tint not restored: %08X", painted));
        cmd(world, "kill @e[type=minecraft:iron_golem]");
        ctx.waitTicks(30);
        cmd(world, "kill @e[type=minecraft:item]");
        ctx.waitTicks(30); // death smoke gone
    }

    private static void hurtShot(ClientGameTestContext ctx, TestSingleplayerContext world, String shot, Consumer<PvpModule> setup) {
        ctx.runOnClient(mc -> {
            reset(m());
            setup.accept(m());
        });
        ctx.waitTicks(12); // the previous flash is over
        cmd(world, "damage @e[type=minecraft:iron_golem,limit=1] 1");
        ctx.waitTicks(2);
        shot(ctx, shot);
    }

    // ============================================================================================ low fire

    private static void lowFire(ClientGameTestContext ctx, TestSingleplayerContext world, int x, int y, int z) {
        cmd(world, "gamemode survival @a");
        cmd(world, "effect give @a minecraft:fire_resistance infinite 0 true");
        cmd(world, "effect give @a minecraft:resistance infinite 4 true");
        look(ctx, 180, 0);
        ignite(ctx, world);
        boolean burning = ctx.computeOnClient(mc -> mc.player.isOnFire());
        if (!burning) throw new AssertionError("player should be burning");
        shot(ctx, "pvp_60_fire_vanilla");
        ctx.runOnClient(mc -> m().lowFire.set(true));
        ignite(ctx, world);
        shot(ctx, "pvp_61_fire_low_default");
        ctx.runOnClient(mc -> m().fireHeight.set(100));
        ignite(ctx, world);
        shot(ctx, "pvp_62_fire_low_100");
        world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().forEach(pl -> pl.clearFire()));
        cmd(world, "effect clear @a");
        cmd(world, "gamemode creative @a");
        ctx.runOnClient(mc -> reset(m()));
        ctx.waitTicks(5);
    }

    private static void ignite(ClientGameTestContext ctx, TestSingleplayerContext world) {
        world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().forEach(pl -> pl.igniteForSeconds(20)));
        ctx.waitTicks(4);
        String state = ctx.computeOnClient(mc -> mc.player.isOnFire() + " ticks=" + mc.player.getRemainingFireTicks()
                + " offset=" + dev.henny.vibeyclient.module.pvp.PvpHooks.fireOffset());
        System.out.println(P + "burning: " + state);
        if (!ctx.computeOnClient(mc -> mc.player.isOnFire())) throw new AssertionError("player should be burning");
    }

    // ============================================================================================ shield

    private static void shield(ClientGameTestContext ctx, TestSingleplayerContext world, int x, int y, int z) {
        cmd(world, "item replace entity @a weapon.mainhand with minecraft:diamond_sword");
        cmd(world, "item replace entity @a weapon.offhand with minecraft:shield");
        look(ctx, 180, 0);
        ctx.waitTicks(50); // equip animation, item name fades
        shot(ctx, "pvp_70_shield_vanilla");
        ctx.runOnClient(mc -> m().lowShield.set(true));
        ctx.waitTicks(3);
        shot(ctx, "pvp_71_shield_low");
        ctx.getInput().holdKey(mc -> mc.keyUse);
        ctx.waitTicks(12);
        shot(ctx, "pvp_72_shield_low_blocking");
        boolean blocking = ctx.computeOnClient(mc -> mc.player.isBlocking());
        System.out.println(P + "blocking with lowered shield: " + blocking);
        ctx.runOnClient(mc -> m().lowShield.set(false));
        ctx.waitTicks(3);
        shot(ctx, "pvp_73_shield_vanilla_blocking");
        ctx.getInput().releaseKey(mc -> mc.keyUse);
        cmd(world, "clear @a");
        ctx.waitTicks(5);
    }

    // ============================================================================================ toggle sprint

    private static void toggleSprint(ClientGameTestContext ctx, TestSingleplayerContext world, int x, int y, int z) {
        cmd(world, "tp @a %.1f %d %.1f 0 0", x + 0.5, y + 1, z - 10 + 0.5); // face south, lots of room
        ctx.waitTicks(5);
        look(ctx, 0, 10);
        ctx.runOnClient(mc -> {
            reset(m());
            m().toggleSprint.set(true);
            m().toggleSneak.set(true);
        });
        ctx.waitTicks(2);
        ctx.getInput().pressKey(mc -> mc.keySprint);
        ctx.waitTicks(2);
        if (!ctx.computeOnClient(mc -> ToggleMovement.sprintHeld())) throw new AssertionError("sprint should be toggled on");
        ctx.getInput().holdKey(mc -> mc.keyUp);
        ctx.waitTicks(8);
        boolean sprinting = ctx.computeOnClient(mc -> mc.player.isSprinting());
        shot(ctx, "pvp_80_toggle_sprint_hud");
        ctx.getInput().releaseKey(mc -> mc.keyUp);
        if (!sprinting) throw new AssertionError("player should sprint with toggled sprint and W");
        ctx.getInput().pressKey(mc -> mc.keySprint);
        ctx.waitTicks(2);
        if (ctx.computeOnClient(mc -> ToggleMovement.sprintHeld())) throw new AssertionError("second press should stop it");
        ctx.getInput().pressKey(mc -> mc.keyShift);
        ctx.waitTicks(4);
        boolean sneaking = ctx.computeOnClient(mc -> mc.player.isShiftKeyDown());
        shot(ctx, "pvp_81_toggle_sneak_hud");
        if (!sneaking) throw new AssertionError("toggled sneak should hold the sneak key");
        ctx.getInput().pressKey(mc -> mc.keyShift);
        ctx.waitTicks(2);
        // the indicator in the HUD editor
        ctx.runOnClient(mc -> {
            dev.henny.vibeyclient.ui.hud.HudLayout.setFlag("intro", true); // the first-run intro is HudGameTest's
            dev.henny.vibeyclient.ui.hud.HudLayout.openEditor(null);
        });
        ctx.waitTicks(6);
        shot(ctx, "pvp_82_hud_editor_indicator");
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> {
            ToggleMovement.reset();
            reset(m());
        });
        cmd(world, "tp @a %.1f %d %.1f 180 0", x + 0.5, y + 1, z + 0.5);
        ctx.waitTicks(5);
    }

    // ============================================================================================ server rules

    private static void serverRule(ClientGameTestContext ctx) {
        look(ctx, 180, -9);
        ctx.runOnClient(mc -> ServerRules.apply(List.of("zoom", "hud.*"), "Tournament rules: no zoom"));
        ctx.getInput().holdKey(m().zoomKey.mapping());
        ctx.waitTicks(15);
        double f = ctx.computeOnClient(mc -> Zoom.factor());
        shot(ctx, "pvp_90_rule_zoom_key_does_nothing");
        ctx.getInput().releaseKey(m().zoomKey.mapping());
        if (f != 1) throw new AssertionError("zoom must not work while the server disables it, factor " + f);
        boolean hud = ctx.computeOnClient(mc -> m().hudActive());
        if (hud) throw new AssertionError("hud.* should disable the indicator");
        ModulesPanel.preselect("pvp");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        shot(ctx, "pvp_91_rule_page_badge");
        ctx.runOnClient(mc -> m().toggleSprint.set(true));
        ctx.waitTicks(3);
        scrollMiddle(ctx, -6);
        shot(ctx, "pvp_92_rule_page_badge_scrolled");
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
        ModulesPanel.preselect("pvp");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        shot(ctx, "pvp_93_rule_page_badge_de");
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        ctx.setScreen(() -> null);
        // rules lifted: works again right away
        ctx.runOnClient(mc -> ServerRules.clear());
        ctx.getInput().holdKey(m().zoomKey.mapping());
        f = settle(ctx, 4);
        ctx.getInput().releaseKey(m().zoomKey.mapping());
        settle(ctx, 1);
        if (Math.abs(f - 4) > 0.05) throw new AssertionError("zoom should work again after the rules are cleared, got " + f);
        System.out.println(P + "server rule check ok");
    }

    // ============================================================================================ helpers

    private static PvpModule m() {
        return ModuleManager.get().get(PvpModule.class);
    }

    private static void reset(PvpModule m) {
        m.settings().all().forEach(s -> s.reset());
    }

    private static void cmd(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(args.length == 0 ? format : String.format(Locale.ROOT, format, args));
    }

    /** Turns the player's eyes to a world point. */
    private static void lookAt(ClientGameTestContext ctx, double tx, double ty, double tz) {
        float[] r = ctx.computeOnClient(mc -> {
            var e = mc.player.getEyePosition();
            double dx = tx - e.x, dy = ty - e.y, dz = tz - e.z;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            return new float[] {yaw, pitch};
        });
        look(ctx, r[0], r[1]);
    }

    private static void look(ClientGameTestContext ctx, float yaw, float pitch) {
        ctx.runOnClient(mc -> {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;
            mc.player.setYHeadRot(yaw);
        });
        ctx.waitTicks(2);
    }

    /** Screenshot without the command feedback in the chat. */
    private static void shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
        ctx.waitTicks(1);
        ctx.takeScreenshot(name);
    }

    private static void scrollMiddle(ClientGameTestContext ctx, double amount) {
        int[] wh = ctx.computeOnClient(mc -> new int[] {mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight()});
        ctx.getInput().setCursorPos(wh[0] / 2.0, wh[1] / 2.0);
        ctx.waitTicks(1);
        ctx.getInput().scroll(amount);
        ctx.waitTicks(4);
        ctx.getInput().setCursorPos(0, 0);
    }

    private static void guiScale(ClientGameTestContext context, int scale) {
        context.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
    }

    @SuppressWarnings("unused")
    private static Minecraft mc() {
        return Minecraft.getInstance();
    }
}

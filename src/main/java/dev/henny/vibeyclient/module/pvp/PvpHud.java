package dev.henny.vibeyclient.module.pvp;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import net.minecraft.client.Minecraft;

/**
 * The module's only HUD element: "Sprinting (toggled)" / "Sneaking (toggled)". All HUD API use of the pvp module is
 * in this class (register + draw), so moving to a new HUD widget API only touches this file.
 */
final class PvpHud {
    private PvpHud() {}

    static final String ID = "pvp.togglestate";
    private static final int PAD = 4, H = 16;
    private static HudElement element;

    static void register(PvpModule m) {
        element = HudLayout.register(HudElement.builder(ID)
                .label("pvp.hud.togglestate")
                .size(PvpHud::width, () -> H)
                .defaults(Anchor.TOP_LEFT, 4, 4)
                .scale(1f, 0.5f, 3f)
                .visibleWhen(m::hudActive)
                .render((g, w, h, preview) -> {
                    String text = text(preview);
                    if (text != null) plate(g, text, w, h);
                })
                .build());
    }

    static void draw(Gfx g) {
        PvpModule m = PvpModule.get();
        if (element == null || m == null || !m.hudActive() || text(false) == null) return;
        HudLayout.draw(g, element);
    }

    /** The line to show, or null when nothing is toggled (the editor shows a sample). */
    static String text(boolean preview) {
        PvpModule m = PvpModule.get();
        if (m != null) {
            if (m.sneakActive() && ToggleMovement.sneakHeld()) return Lang.tr("pvp.hud.sneaking");
            if (m.sprintActive() && ToggleMovement.sprintHeld()) {
                var p = Minecraft.getInstance().player;
                return Lang.tr(p != null && p.isSprinting() ? "pvp.hud.sprinting" : "pvp.hud.sprintReady");
            }
        }
        return preview ? Lang.tr("pvp.hud.sprinting") : null;
    }

    private static int width() {
        String t = text(true);
        return Minecraft.getInstance().font.width(t) + PAD * 2 + 6;
    }

    private static void plate(Gfx g, String text, int w, int h) {
        g.rect(0, 0, w, h, 0x99000000);
        g.rect(0, 0, 2, h, Theme.BRAND);
        g.text(text, PAD + 4, (h - 8) / 2, Theme.TEXT);
    }
}

package dev.henny.vibeyclient.module.pvp;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudIcons;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudOption;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.TextWidget;
import net.minecraft.client.Minecraft;

/**
 * The module's HUD widget: "Sprinting (toggled)" / "Sneaking (toggled)", on the core widget system (docs/HUD.md):
 * the core draws, styles and saves it and hides it when the server disables {@code hud.togglestate}. The player
 * moves and styles it in the HUD editor like every other widget.
 */
final class PvpHud {
    private PvpHud() {}

    static final String ID = "pvp.togglestate";

    static void register(PvpModule m) {
        HudLayout.register(HudElement.builder(ID)
                .label("pvp.hud.togglestate")
                .description("pvp.hud.togglestate.desc")
                .icon(HudIcons.SPEED)
                .category(HudElement.Category.MOVEMENT)
                .rules(PvpModule.RULE_HUD)
                .defaults(Anchor.BOTTOM_LEFT, 4, -4)
                .scale(1f, 0.5f, 3f)
                .visibleWhen(() -> m.isEnabled() && m.showToggleState.on())
                .options(HudOption.toggle("icon", "pvp.hud.togglestate.icon", true))
                .widget(Indicator::new)
                .build());
    }

    /** The line to show, or null when nothing is toggled. */
    static String text() {
        PvpModule m = PvpModule.get();
        if (m == null) return null;
        if (m.sneakActive() && ToggleMovement.sneakHeld()) return Lang.tr("pvp.hud.sneaking");
        if (m.sprintActive() && ToggleMovement.sprintHeld()) {
            var p = Minecraft.getInstance().player;
            return Lang.tr(p != null && p.isSprinting() ? "pvp.hud.sprinting" : "pvp.hud.sprintReady");
        }
        return null;
    }

    static final class Indicator extends TextWidget {
        @Override
        protected int rate() {
            return 10;
        }

        @Override
        protected void lines(HudPaint p) {
            String t = text();
            if (t == null && p.preview) t = Lang.tr("pvp.hud.sprinting");
            if (t != null) line(null, t);
        }

        @Override
        protected String icon(HudPaint p) {
            return p.flag("icon") ? HudIcons.SPEED : null;
        }
    }
}

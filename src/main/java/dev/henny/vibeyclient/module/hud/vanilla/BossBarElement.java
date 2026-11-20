package dev.henny.vibeyclient.module.hud.vanilla;

import dev.henny.vibeyclient.module.hud.mixin.BossOverlayAccessor;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.BooleanSupplier;

final class BossBarElement {
    private BossBarElement() {}

    static final String ID = "hud.bossbar";
    static final int W = 182;
    static final int ROW = 19;

    static HudElement build(BooleanSupplier enabled) {
        return HudElement.builder(ID).label("hud.widget.bossbar").description("hud.widget.bossbar.desc")
                .icon("hud_media").category(HudElement.Category.WORLD).defaults(Anchor.TOP, 0, 3).scale(1f, 0.5f, 2f)
                .size(() -> W, () -> ROW * Math.max(1, count())).startHidden().render(BossBarElement::preview).visibleWhen(enabled).build();
    }

    static int count() {
        try {
            return ((BossOverlayAccessor) Minecraft.getInstance().gui.getBossOverlay()).vibey$events().size();
        } catch (RuntimeException e) {
            return 1;
        }
    }

    static void draw(GuiGraphics g, DeltaTracker tick, net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement vanilla, HudElement e) {
        Placed.draw(g, e, -(g.guiWidth() / 2f - W / 2f), -3, () -> vanilla.render(g, tick));
    }

    private static void preview(Gfx g, int w, int h, boolean preview) {
        if (!preview) return;
        String name = "Ender Dragon";
        g.textShadow(name, (W - g.width(name)) / 2, 0, 0xFFFFFFFF);
        g.rect(0, 9, W, 5, 0xFF2A1430);
        g.rect(0, 9, W * 7 / 10, 5, 0xFFE44CD6);
        g.rect(0, 9, W, 1, 0x40FFFFFF);
    }
}

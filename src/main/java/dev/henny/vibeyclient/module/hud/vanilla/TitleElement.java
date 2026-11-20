package dev.henny.vibeyclient.module.hud.vanilla;

import dev.henny.vibeyclient.module.hud.mixin.GuiTitleAccessor;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

final class TitleElement {
    private TitleElement() {}

    static final String ID = "hud.title";
    static final int TOP = 42;
    static final int H = 72;
    private static final String SAMPLE = "Title";
    private static final String SAMPLE_SUB = "subtitle";

    static HudElement build(BooleanSupplier enabled) {
        return HudElement.builder(ID).label("hud.widget.title").description("hud.widget.title.desc").icon("hud_template")
                .category(HudElement.Category.WORLD).defaults(Anchor.CENTER, 0, -6).scale(1f, 0.25f, 2f)
                .size(TitleElement::width, () -> H).startHidden().render(TitleElement::preview).visibleWhen(enabled).build();
    }

    static int width() {
        Font f = Minecraft.getInstance().font;
        GuiTitleAccessor gui = (GuiTitleAccessor) Minecraft.getInstance().gui;
        Component t = gui.vibey$title(), s = gui.vibey$subtitle();
        int tw = t == null ? f.width(SAMPLE) : f.width(t);
        int sw = s == null ? (t == null ? f.width(SAMPLE_SUB) : 0) : f.width(s);
        return Math.max(tw * 4, sw * 2) + 8;
    }

    static void draw(GuiGraphics g, DeltaTracker tick, net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement vanilla, HudElement e) {
        float w = width();
        Placed.draw(g, e, -(g.guiWidth() / 2f - w / 2f), -(g.guiHeight() / 2f - TOP), () -> vanilla.render(g, tick));
    }

    private static void preview(Gfx g, int w, int h, boolean preview) {
        if (!preview) return;
        int tw = g.width(SAMPLE) * 4, sw = g.width(SAMPLE_SUB) * 2;
        g.textScaled(SAMPLE, (w - tw) / 2, TOP - 40, 4, 0xFFFFFFFF, false);
        g.textScaled(SAMPLE_SUB, (w - sw) / 2, TOP + 10, 2, 0xFFDDDDDD, false);
    }
}

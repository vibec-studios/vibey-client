package dev.henny.vibeyclient.module.info.tool;

import dev.henny.vibeyclient.module.info.card.ItemDraw;
import dev.henny.vibeyclient.module.info.core.Colors;
import dev.henny.vibeyclient.module.info.core.Flash;
import dev.henny.vibeyclient.module.info.core.WarnLevel;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ToolWarnWidget extends HudWidget {
    private static final int PAD = 3, ICON = 16;
    private static ItemStack sample;

    private ItemStack stack = ItemStack.EMPTY;
    private WarnLevel level = WarnLevel.NONE;
    private String text = "";

    @Override
    protected int rate() {
        return 10;
    }

    @Override
    protected void update(HudPaint p) {
        stack = ToolState.stack();
        level = ToolState.level();
        int left = ToolState.remaining();
        if (level == WarnLevel.NONE && p.preview) {
            if (sample == null) sample = new ItemStack(Items.DIAMOND_PICKAXE);
            stack = sample;
            level = WarnLevel.LOW;
            left = 12;
        }
        text = Integer.toString(left);
        width = PAD + ICON + 3 + p.g.width(text) + PAD;
        height = ICON + PAD * 2;
    }

    @Override
    protected boolean shown(HudPaint p) {
        return level != WarnLevel.NONE;
    }

    @Override
    protected void paint(HudPaint p) {
        if (level == WarnLevel.NONE) return;
        float a = p.preview ? 1f : Flash.alpha(p.now, level);
        int color = level == WarnLevel.CRITICAL ? Theme.DANGER : Theme.WARN;
        p.background();
        p.fillShape(0, 0, p.w, p.h, Colors.alpha(color, 0.22f * a));
        p.outlineShape(0, 0, p.w, p.h, Colors.alpha(color, a));
        ItemDraw.full(p.g, stack, PAD, PAD);
        p.plain(text, PAD + ICON + 3, (p.h - 8) / 2, Colors.alpha(color, Math.max(0.6f, a)));
    }
}

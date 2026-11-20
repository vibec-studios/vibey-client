package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.module.info.pin.PinControl;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.world.item.ItemStack;

public final class InfoCardWidget extends HudWidget {
    private static final int PAD = 4, ICON = 16, GAP = 4, LINE = 10, MAX_TEXT = 160;

    private CardData data;
    private String title = "";
    private String mod;
    private String value;
    private String mark;
    private String valueIcon;
    private ItemStack tool = ItemStack.EMPTY;
    private int markColor, valueColor, textX, modY, statusY;

    @Override
    protected int rate() {
        return 20;
    }

    @Override
    protected void update(HudPaint p) {
        CardData d = CardState.current();
        if (d == null && p.preview) d = CardSamples.block();
        data = d;
        if (d == null) {
            width = 60;
            height = ICON + PAD * 2;
            return;
        }
        title = p.g.clip(d.title(), MAX_TEXT);
        mod = d.mod() == null || d.mod().isEmpty() ? null : p.g.clip(d.mod(), MAX_TEXT);
        textX = PAD + ICON + GAP;
        modY = PAD + LINE;
        statusY = mod == null ? modY : modY + LINE;
        status(d);
        int w = Math.max(p.g.width(title), mod == null ? 0 : p.g.width(mod));
        if (d.hasStatus()) w = Math.max(w, statusWidth(p));
        width = textX + w + PAD;
        int bottom = d.hasStatus() ? statusY + rowHeight() : (mod != null ? modY + 9 : PAD + 9);
        height = Math.max(PAD * 2 + ICON, bottom + PAD);
    }

    private void status(CardData d) {
        tool = d.hasHarvest() ? ToolIcons.of(d.harvest()) : ItemStack.EMPTY;
        mark = d.hasHarvest() ? (d.harvest().canHarvest() ? Icons.CHECK : Icons.CLOSE) : null;
        markColor = d.hasHarvest() && d.harvest().canHarvest() ? Theme.OK : Theme.DANGER;
        valueIcon = null;
        value = null;
        if (d.hasGrowth()) {
            valueIcon = InfoIcons.SPROUT;
            value = d.growth() + "%";
            valueColor = d.mature() ? Theme.OK : Theme.TEXT;
        } else if (d.health() != null) {
            valueIcon = InfoIcons.HEART;
            value = d.health();
            valueColor = Theme.TEXT;
        }
    }

    private int statusWidth(HudPaint p) {
        int w = 0;
        if (!tool.isEmpty()) w += ICON + 1;
        if (mark != null) w += 9 + GAP;
        if (value != null) w += 10 + p.g.width(value);
        return w;
    }

    private int rowHeight() {
        return tool.isEmpty() ? 9 : ICON;
    }

    @Override
    protected boolean shown(HudPaint p) {
        return data != null;
    }

    @Override
    protected void paint(HudPaint p) {
        if (data == null) return;
        p.background();
        ItemDraw.full(p.g, data.icon(), PAD, PAD);
        p.text(title, textX, PAD + 1);
        if (mod != null) p.plain(mod, textX, modY + 1, Theme.MUTED);
        int x = textX;
        int iconY = statusY + (rowHeight() - 9) / 2;
        if (!tool.isEmpty()) {
            ItemDraw.full(p.g, tool, x, statusY);
            x += ICON + 1;
        }
        if (mark != null) {
            Icons.draw(p.g, mark, x, iconY, markColor);
            x += 9 + GAP;
        }
        if (value != null) {
            Icons.draw(p.g, valueIcon, x, iconY, valueIcon.equals(InfoIcons.HEART) ? Theme.DANGER : Theme.OK);
            p.plain(value, x + 10, iconY + 1, valueColor);
        }
        float hold = PinControl.progress();
        if (hold > 0f) p.g.rect(0, p.h - 1, Math.round(p.w * hold), 1, Theme.BRAND);
    }
}

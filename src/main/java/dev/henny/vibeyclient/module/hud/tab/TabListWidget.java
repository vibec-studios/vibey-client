package dev.henny.vibeyclient.module.hud.tab;

import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;

public final class TabListWidget extends HudWidget {
    static final int ROW = 10;
    static final int PER_COLUMN = 20;
    static final int PAD = 3;

    private final TabRows rows = new TabRows();
    private boolean heads;
    private boolean ms;
    private int columns = 1;
    private int colW;
    private int perCol;

    @Override
    protected int rate() {
        return 2;
    }

    public static boolean held() {
        Minecraft mc = Minecraft.getInstance();
        return mc.options.keyPlayerList.isDown();
    }

    @Override
    protected void update(HudPaint p) {
        rows.read(p.option("sort"), p.preview);
        heads = p.flag("heads");
        ms = p.flag("ms");
        Font f = p.g.font;
        int n = rows.names.size();
        columns = Math.max(1, (n + PER_COLUMN - 1) / PER_COLUMN);
        perCol = Math.max(1, (n + columns - 1) / columns);
        int name = 0;
        for (int i = 0; i < n; i++) name = Math.max(name, f.width(rows.names.get(i)));
        colW = (heads ? 10 : 0) + name + 4 + PingBars.WIDTH + (ms ? f.width("999ms") + 3 : 0);
        width = columns * colW + (columns - 1) * 4 + PAD * 2;
        height = Math.max(1, perCol) * ROW + PAD * 2 - 1;
    }

    @Override
    protected boolean shown(HudPaint p) {
        return held() && !rows.names.isEmpty();
    }

    @Override
    protected void paint(HudPaint p) {
        p.background();
        GuiGraphics g = p.g.g;
        Font f = p.g.font;
        for (int i = 0; i < rows.names.size(); i++) {
            int col = i / perCol, row = i % perCol;
            int x = PAD + col * (colW + 4), y = PAD + row * ROW;
            if (row % 2 == 1) g.fill(x - 1, y - 1, x + colW + 1, y + ROW - 1, 0x14FFFFFF);
            int tx = x;
            if (heads) {
                PlayerFaceRenderer.draw(g, rows.skins.get(i), x, y, 8);
                tx += 10;
            }
            g.drawString(f, rows.names.get(i), tx, y, p.style.text, p.style.shadow);
            int ping = rows.pings.get(i);
            int bx = x + colW - PingBars.WIDTH;
            PingBars.draw(g, bx, y, ping);
            if (ms) {
                String s = rows.labels.get(i);
                p.dimText(s, bx - 3 - f.width(s), y, 0.7f);
            }
        }
    }
}

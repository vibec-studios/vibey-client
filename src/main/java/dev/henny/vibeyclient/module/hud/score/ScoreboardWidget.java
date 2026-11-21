package dev.henny.vibeyclient.module.hud.score;

import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class ScoreboardWidget extends HudWidget {
    static final int LINE = 9;
    static final int PAD = 2;

    private final ScoreLines lines = new ScoreLines();
    private boolean has;
    private boolean numbers;
    private boolean header;
    private int titleW;

    @Override
    protected void update(HudPaint p) {
        has = lines.read();
        if (!has && p.preview) {
            lines.sample();
            has = true;
        }
        numbers = p.flag("numbers");
        header = p.flag("title");
        Font f = p.g.font;
        titleW = header && lines.title != null ? f.width(lines.title) : 0;
        int w = titleW;
        for (int i = 0; i < lines.names.size(); i++) {
            int sw = numbers ? f.width(lines.scores.get(i)) + 6 : 0;
            w = Math.max(w, f.width(lines.names.get(i)) + sw);
        }
        width = w + PAD * 2;
        height = (lines.names.size() + (header ? 1 : 0)) * LINE + PAD * 2 + (header ? 1 : 0);
    }

    @Override
    protected boolean shown(HudPaint p) {
        return has;
    }

    @Override
    protected void paint(HudPaint p) {
        if (!has) return;
        p.background();
        int y = PAD;
        if (header) {
            p.fillShape(0, 0, p.w, LINE + PAD + 1, 0x33000000);
            draw(p, lines.title, (p.w - titleW) / 2, y);
            y += LINE + 1;
        }
        Font f = p.g.font;
        for (int i = 0; i < lines.names.size(); i++) {
            draw(p, lines.names.get(i), PAD, y);
            if (numbers) {
                Component s = lines.scores.get(i);
                draw(p, s, p.w - PAD - f.width(s), y);
            }
            y += LINE;
        }
    }

    private static void draw(HudPaint p, Component c, int x, int y) {
        p.g.text(c, x, y, p.style.text, p.style.shadow);
    }
}

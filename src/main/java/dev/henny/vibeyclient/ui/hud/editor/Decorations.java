package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.edit.EditSession;
import dev.henny.vibeyclient.ui.hud.edit.PointerTool;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.VuiRuntime;
import dev.henny.vui.mc.draw.McCanvas;

final class Decorations {
    private final Pen pen = new Pen();
    private final Frames frames = new Frames();
    private final GuideLines guides = new GuideLines();
    private final HudLayout.Box tmp = new HudLayout.Box();

    void under(Object graphics, EditSession s, PointerTool tool, boolean inWorld) {
        Pen p = begin(graphics);
        try {
            p.rect(0, 0, s.width(), s.height(), inWorld ? 0x33000000 : 0x900A0A0E);
            frames.grid(p, s.width(), s.height(), tool.busy());
        } finally {
            end();
        }
    }

    void over(Object graphics, EditSession s, PointerTool tool) {
        Pen p = begin(graphics);
        try {
            p.canvas().layer();
            frames.elements(p, s, tool);
            guides.draw(p, s.guides, tool, s.width(), s.height());
            HudElement e = s.selection.primary();
            if (tool.mode() == PointerTool.Mode.RESIZE && e != null) {
                HudLayout.Box b = s.box(e, tmp);
                float y = b.y + b.h + 4 + 12 > s.height() ? b.y - 16 : b.y + b.h + 4;
                guides.sizeChip(p, s.guides, b.x + b.w / 2f, y, s.width());
            }
        } finally {
            end();
        }
    }

    void layer(Object graphics) {
        begin(graphics).canvas().layer();
        end();
    }

    void pointer(Object graphics, double x, double y) {
        Pen p = begin(graphics);
        try {
            p.canvas().layer();
            for (int i = 0; i < 10; i++) {
                int w = i < 7 ? i + 1 : 10 - i;
                p.rect((float) x - 1, (float) y + i - 1, w + 2, 1, 0xFF000000);
                p.rect((float) x, (float) y + i, w, 1, 0xFFFFFFFF);
            }
            p.rect((float) x + 3, (float) y + 9, 2, 3, 0xFFFFFFFF);
        } finally {
            end();
        }
    }

    private Pen begin(Object graphics) {
        McCanvas c = VuiRuntime.get().canvas().begin(graphics);
        return pen.begin(c, VuiMc.vui().backend().text());
    }

    private static void end() {
        VuiRuntime.get().canvas().end();
    }
}

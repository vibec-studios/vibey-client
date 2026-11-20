package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.edit.EditSession;
import dev.henny.vibeyclient.ui.hud.edit.Handles;
import dev.henny.vibeyclient.ui.hud.edit.PointerTool;

final class Frames {
    private final HudLayout.Box tmp = new HudLayout.Box();
    private final HudLayout.Box group = new HudLayout.Box();

    void grid(Pen p, int sw, int sh, boolean busy) {
        int c = busy ? 0x1CFFFFFF : 0x0EFFFFFF;
        p.rect(sw / 3, 0, 1, sh, c);
        p.rect(sw * 2 / 3, 0, 1, sh, c);
        p.rect(0, sh / 3, sw, 1, c);
        p.rect(0, sh * 2 / 3, sw, 1, c);
        int m = busy ? 0x40FFFFFF : 0x1AFFFFFF;
        for (int y = 0; y < sh; y += 6) p.rect(sw / 2, y, 1, 3, m);
        for (int x = 0; x < sw; x += 6) p.rect(x, sh / 2, 3, 1, m);
    }

    void elements(Pen p, EditSession s, PointerTool tool) {
        for (HudElement e : s.canvas()) {
            HudLayout.Box b = s.box(e, tmp);
            int x = (int) Math.floor(b.x), y = (int) Math.floor(b.y);
            int w = Math.max(4, Math.round(b.w)), h = Math.max(4, Math.round(b.h));
            if (HudLayout.serverDisabled(e)) {
                p.rect(x, y, w, h, 0xC8141418);
                for (int ly = y + 2; ly < y + h - 1; ly += 3) p.rect(x + 1, ly, w - 2, 1, 0x12FFFFFF);
            }
            if (s.selection.contains(e)) p.outline(x - 1, y - 1, w + 2, h + 2, Theme.BRAND);
            else if (e == tool.hovered()) p.outline(x - 1, y - 1, w + 2, h + 2, Theme.alpha(Theme.BRAND, 0.7f));
            else p.dashed(x - 1, y - 1, w + 2, h + 2, 0x30FFFFFF);
        }
        HudElement hov = tool.hovered();
        if (hov != null && !s.selection.contains(hov) && tool.mode() == PointerTool.Mode.IDLE) nameTag(p, s, hov);
        if (s.selection.size() > 1) {
            s.groupBox(group);
            p.dashed(Math.round(group.x) - 3, Math.round(group.y) - 3, Math.round(group.w) + 6, Math.round(group.h) + 6, Theme.alpha(Theme.BRAND, 0.9f));
        }
        if (s.selection.size() == 1 && tool.mode() != PointerTool.Mode.MOVE) handles(p, s.box(s.selection.primary(), tmp), tool.handle());
    }

    private void nameTag(Pen p, EditSession s, HudElement e) {
        HudLayout.Box b = s.box(e, tmp);
        String label = e.displayName();
        float tw = p.width(label) + 8;
        float ty = b.y - 13 >= 0 ? b.y - 13 : b.y + b.h + 2;
        float tx = Math.max(0, Math.min(s.width() - tw, b.x - 1));
        p.chip(label, Math.round(tx), Math.round(ty), 0xE60B0B0E, Theme.TEXT);
    }

    private static void handles(Pen p, HudLayout.Box b, int hot) {
        int x = Handles.left(b), y = Handles.top(b), w = Handles.width(b), h = Handles.height(b);
        for (int i = 0; i < 8; i++) {
            if (!Handles.visible(i, w, h)) continue;
            int sz = i == hot ? Handles.SIZE + 2 : Handles.SIZE;
            int cx = Handles.x(i, x, w) - sz / 2, cy = Handles.y(i, y, h) - sz / 2;
            p.rect(cx, cy, sz, sz, Theme.BRAND);
            p.rect(cx + 1, cy + 1, sz - 2, sz - 2, i == hot ? Theme.BRAND2 : 0xFFFFFFFF);
        }
    }
}

package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudStyle;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * W A S D, jump, and the mouse buttons with their clicks per second. Keys light up in the accent colour with a short
 * eased fade (time based, no allocation); key labels follow the player's bindings.
 */
final class KeystrokesWidget extends HudWidget {
    private static final int K = 18, GAP = 2;
    private static final int W = 0, A = 1, S = 2, D = 3, JUMP = 4, LMB = 5, RMB = 6;

    private final String[] labels = new String[7];
    private final float[] glow = new float[7];
    private long last;
    private String lmbCps = "", rmbCps = "";
    private boolean mouse, space, cps;

    @Override
    protected int rate() {
        return 10;
    }

    @Override
    protected void update(HudPaint p) {
        Minecraft mc = Minecraft.getInstance();
        mouse = p.flag("mouse");
        space = p.flag("space");
        cps = p.flag("cps") && p.detailed();
        if (mc.options != null) {
            labels[W] = keyLabel(mc.options.keyUp, "W");
            labels[A] = keyLabel(mc.options.keyLeft, "A");
            labels[S] = keyLabel(mc.options.keyDown, "S");
            labels[D] = keyLabel(mc.options.keyRight, "D");
        }
        labels[JUMP] = "";
        labels[LMB] = Lang.tr("hud.label.lmb");
        labels[RMB] = Lang.tr("hud.label.rmb");
        boolean sample = p.preview && mc.level == null;
        lmbCps = (sample ? 8 : Clicks.left()) + " " + Lang.tr("hud.label.cps");
        rmbCps = (sample ? 3 : Clicks.right()) + " " + Lang.tr("hud.label.cps");
        width = K * 3 + GAP * 2;
        height = K * 2 + GAP + (mouse ? GAP + (cps ? K + 4 : K) : 0) + (space ? GAP + 9 : 0);
    }

    private static String keyLabel(KeyMapping k, String def) {
        try {
            String s = k.getTranslatedKeyMessage().getString();
            if (s.isEmpty()) return def;
            return s.length() > 3 ? s.substring(0, 3) : s.toUpperCase(java.util.Locale.ROOT);
        } catch (RuntimeException e) {
            return def;
        }
    }

    @Override
    protected void paint(HudPaint p) {
        Minecraft mc = Minecraft.getInstance();
        long now = p.now;
        float dt = last == 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        boolean inGame = ClientCompat.screen() == null || p.preview;
        boolean demo = p.preview && mc.level == null;
        // in the editor with nothing pressed, a gentle demo so it does not look dead
        int demoKey = demo ? (int) ((now / 450) % 4) : -1;
        boolean[] down = DOWN;
        down[W] = demoKey == 0 || (inGame && mc.options.keyUp.isDown());
        down[A] = demoKey == 1 || (inGame && mc.options.keyLeft.isDown());
        down[S] = demoKey == 2 || (inGame && mc.options.keyDown.isDown());
        down[D] = demoKey == 3 || (inGame && mc.options.keyRight.isDown());
        down[JUMP] = inGame && mc.options.keyJump.isDown();
        down[LMB] = inGame && mc.options.keyAttack.isDown();
        down[RMB] = inGame && mc.options.keyUse.isDown();
        for (int i = 0; i < 7; i++) {
            float target = down[i] ? 1f : 0f;
            // ~90 ms in, ~180 ms out
            float speed = down[i] ? 11f : 5.5f;
            glow[i] += (target - glow[i]) * Math.min(1f, dt * speed);
            if (Math.abs(target - glow[i]) < 0.01f) glow[i] = target;
        }
        int ox = (p.w - width) / 2, oy = (p.h - height) / 2;
        key(p, W, ox + K + GAP, oy, K, K);
        int y = oy + K + GAP;
        key(p, A, ox, y, K, K);
        key(p, S, ox + K + GAP, y, K, K);
        key(p, D, ox + (K + GAP) * 2, y, K, K);
        y += K + GAP;
        if (mouse) {
            int mw = (width - GAP) / 2;
            int mh = cps ? K + 4 : K;
            key(p, LMB, ox, y, mw, mh);
            key(p, RMB, ox + mw + GAP, y, width - mw - GAP, mh);
            if (cps) {
                sub(p, lmbCps, ox, y, mw, mh, LMB);
                sub(p, rmbCps, ox + mw + GAP, y, width - mw - GAP, mh, RMB);
            }
            y += mh + GAP;
        }
        if (space) {
            key(p, JUMP, ox, y, width, 9);
            int bw = width / 3;
            p.g.rect(ox + (width - bw) / 2, y + 4, bw, 1, Theme.alpha(p.style.text, 0.55f));
        }
    }

    private static final boolean[] DOWN = new boolean[7];

    private void key(HudPaint p, int i, int x, int y, int w, int h) {
        HudStyle.Resolved st = p.style;
        float t = glow[i];
        int base = st.background ? st.bg : 0x40000000;
        int lit = Theme.alpha(st.accent, 0.85f);
        int fill = Theme.mix(base, lit, t);
        // pressed keys sink by one pixel
        int sink = t > 0.5f ? 1 : 0;
        p.fillShape(x, y + sink, w, h - sink, fill);
        if (st.border) p.outlineShape(x, y + sink, w, h - sink, Theme.alpha(st.accent, 0.7f));
        String l = labels[i];
        if (l == null || l.isEmpty()) return;
        int tw = p.g.width(l);
        int ty = y + sink + (i >= LMB && cps ? 3 : (h - 8) / 2);
        int color = Theme.mix(st.paint == HudStyle.TextPaint.ACCENT ? st.accent : st.text, 0xFFFFFFFF, t);
        if (st.paint == HudStyle.TextPaint.RAINBOW && t < 0.5f) p.text(l, x + (w - tw) / 2, ty);
        else p.plain(l, x + (w - tw) / 2, ty, color);
    }

    private void sub(HudPaint p, String s, int x, int y, int w, int h, int i) {
        int tw = p.g.width(s);
        float t = glow[i];
        int sink = t > 0.5f ? 1 : 0;
        // small second line: the CPS
        p.g.push();
        p.g.translate(x + w / 2f, y + sink + 12);
        p.g.scale(0.75f, 0.75f);
        p.plain(s, -tw / 2, 0, Theme.alpha(Theme.mix(p.style.text, 0xFFFFFFFF, t), 0.8f));
        p.g.pop();
    }
}

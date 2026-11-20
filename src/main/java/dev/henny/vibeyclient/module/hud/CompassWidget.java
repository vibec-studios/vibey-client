package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

final class CompassWidget extends HudWidget {
    private static final String[] DEG = new String[360];
    private final String[] letters = new String[8];
    private boolean detailed;
    private float demo;

    static {
        for (int i = 0; i < 360; i++) DEG[i] = i + "°";
    }

    @Override
    protected int rate() {
        return 1;
    }

    @Override
    protected void update(HudPaint p) {
        String[] keys = {"hud.dir.n", "hud.dir.ne", "hud.dir.e", "hud.dir.se", "hud.dir.s", "hud.dir.sw", "hud.dir.w", "hud.dir.nw"};
        for (int i = 0; i < 8; i++) letters[i] = Lang.tr(keys[i]);
        detailed = p.detailed();
        width = 180;
        height = detailed ? 22 : 14;
    }

    @Override
    protected void paint(HudPaint p) {
        Player pl = Minecraft.getInstance().player;
        float heading;
        if (pl != null) heading = Directions.heading(pl.getViewYRot(1f));
        else {
            demo = (p.now % 12000L) / 12000f * 360f;
            heading = demo;
        }
        p.background();
        int w = p.w, h = p.h;
        int stripH = detailed ? 14 : h;
        float range = 150f;
        float ppd = w / range;
        int cx = w / 2;
        int mid = stripH / 2;
        float start = heading - range / 2f;
        int first = (int) Math.ceil(start / 15f) * 15;
        for (int d = first; d <= start + range; d += 15) {
            float x = cx + (d - heading) * ppd;
            if (x < 3 || x > w - 3) continue;
            float edge = Math.min(x, w - x) / 24f;
            float a = Math.min(1f, edge);
            int deg = ((d % 360) + 360) % 360;
            int xi = Math.round(x);
            if (deg % 45 == 0) {
                String l = letters[deg / 45];
                boolean cardinal = deg % 90 == 0;
                int tw = p.g.width(l);
                int col = deg == 0 ? Theme.alpha(Theme.DANGER, a) : Theme.alpha(cardinal ? p.style.text : Theme.mix(p.style.text, 0xFF000000, 0.3f), a);
                p.plain(l, xi - tw / 2 + 1, mid - 4, col);
            } else {
                p.g.rect(xi, mid - 2, 1, 4, Theme.alpha(p.style.text, 0.45f * a));
            }
        }
        int ac = p.style.accent;
        p.g.rect(cx, 0, 1, 3, ac);
        p.g.rect(cx - 1, 0, 3, 1, ac);
        p.g.rect(cx, stripH - 3, 1, 3, ac);
        if (detailed) {
            String s = DEG[Math.round(heading) % 360];
            p.accentText(s, cx - p.g.width(s) / 2 + 1, stripH);
        }
    }
}

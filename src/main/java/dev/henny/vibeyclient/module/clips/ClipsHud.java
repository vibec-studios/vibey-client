package dev.henny.vibeyclient.module.clips;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudIcons;
import dev.henny.vibeyclient.ui.hud.HudLayout;

/**
 * The recording indicator {@code clips.indicator}: a small red dot with "REC" while the buffer records, amber
 * "Paused" while it is paused. A plain HUD element drawn by the module (so HUD templates, which only manage widgets,
 * never hide it); the player can still move and scale it in the HUD editor.
 */
final class ClipsHud {
    private ClipsHud() {}

    static final String ID = "clips.indicator";
    private static HudElement element;
    private static String rec = "", paused = "";
    private static String lang;

    static void register(ClipsModule m) {
        element = HudLayout.register(HudElement.builder(ID)
                .label("clips.hud.indicator")
                .description("clips.hud.indicator.desc")
                .icon(HudIcons.FPS)
                .category(HudElement.Category.MEDIA)
                .rules(ClipsModule.RULE_CLIPS)
                .defaults(Anchor.BOTTOM_RIGHT, -4, -4) // top right is where toasts appear
                .scale(1f, 0.5f, 3f)
                .size(58, 12)
                .visibleWhen(m::indicatorShown)
                .render(ClipsHud::paint)
                .build());
    }

    /** Module HUD pass: draws the dot when there is something to show. */
    static void render(Gfx g, ClipsModule m) {
        if (element == null || !m.indicatorShown()) return;
        Recorder.State s = m.recorder.state();
        if (s != Recorder.State.RECORDING && s != Recorder.State.PAUSED && s != Recorder.State.STARTING) return;
        HudLayout.draw(g, element);
    }

    private static void paint(Gfx g, int w, int h, boolean preview) {
        String l = Lang.tr("clips.hud.rec");
        if (!l.equals(lang)) {
            lang = l;
            rec = l;
            paused = Lang.tr("clips.hud.paused");
        }
        ClipsModule m = ClipsModule.get();
        Recorder.State s = m == null ? Recorder.State.OFF : m.recorder.state();
        boolean isPaused = s == Recorder.State.PAUSED && !preview;
        String text = isPaused ? paused : rec;
        int color = isPaused ? Theme.WARN : 0xFFFF3B3B;
        if (s == Recorder.State.STARTING && (System.currentTimeMillis() / 400) % 2 == 0) color = Theme.alpha(color, 0.35f);
        int tw = g.width(text);
        int bw = 4 + 5 + 3 + tw + 4;
        int x0 = w - bw; // right-aligned inside the box (default anchor is the right edge)
        g.rect(x0, 0, bw, h, 0x90000000);
        int y = (h - 5) / 2;
        g.rect(x0 + 5, y, 3, 5, color);
        g.rect(x0 + 4, y + 1, 5, 3, color);
        g.text(text, x0 + 12, (h - 8) / 2 + 1, 0xFFFFFFFF);
    }
}

package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;

/**
 * Header bar and close button of the full-screen wardrobe screens. The screens use the core immediate-mode
 * {@link Ui} context (one per screen) and {@link Widgets} for everything else.
 */
final class WardrobeChrome {
    private WardrobeChrome() {}

    /** Height of the header bar of the wardrobe screens. */
    static final int HEADER = 28;

    /** The square close button of the header (X). True when clicked. */
    static boolean closeButton(Ui ui, Object key, int x, int y) {
        Gfx g = ui.g;
        boolean clicked = ui.clicked(x, y, 18, 18);
        float hov = ui.anim(key, 2, 0f).to(ui.hover(x, y, 18, 18) || ui.focusedLast() ? 1f : 0f);
        if (hov > 0f) g.rect(x, y, 18, 18, Theme.alpha(Theme.SURFACE3, hov));
        Widgets.cross(g, x + 5, y + 5, 8, Theme.mix(Theme.MUTED, Theme.TEXT, hov));
        return clicked;
    }

    /** Header bar: seal icon, bold title, a dim subtitle. Returns the x after the text. */
    static int header(Ui ui, int width, String title, String subtitle) {
        Gfx g = ui.g;
        g.rect(0, 0, width, HEADER, Theme.SURFACE);
        g.hline(0, HEADER, width, Theme.LINE);
        int x = Theme.PAD;
        g.image(Compat.id("icon.png"), x, (HEADER - 16) / 2, 16, 16, 512);
        x += 22;
        int ty = (HEADER - 8) / 2;
        g.bold(title, x, ty, Theme.TEXT);
        x += g.boldWidth(title) + 8;
        if (subtitle != null && !subtitle.isEmpty()) {
            g.text(subtitle, x, ty, Theme.DIM);
            x += g.width(subtitle) + 8;
        }
        return x;
    }
}

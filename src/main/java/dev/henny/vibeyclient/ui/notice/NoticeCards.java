package dev.henny.vibeyclient.ui.notice;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.backend.Notices;
import dev.henny.vibeyclient.backend.Notices.Notice;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;

import java.util.List;

/**
 * The notices of {@link Announcements} as cards at the top of the General tab: an accent bar (brand / warning
 * colour), the title, the wrapped text, "Open link" when there is a link, and "Dismiss".
 */
public final class NoticeCards {
    private NoticeCards() {}

    /** Last drawn dismiss button per notice id (GUI px x, y, w, h): for the gametest's click. */
    public static final java.util.Map<String, int[]> DISMISS_BOUNDS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Draws the cards from {@code y}; returns the y below them (unchanged when there is nothing to show). */
    public static int draw(Ui ui, int x, int y, int w) {
        List<Notice> list = Announcements.active();
        DISMISS_BOUNDS.clear();
        for (Notice n : list) y = card(ui, n, x, y, w) + 8;
        return list.isEmpty() ? y : y + 4;
    }

    private static int card(Ui ui, Notice n, int x, int y, int w) {
        Gfx g = ui.g;
        int accent = n.level() == Notices.Level.WARN ? Theme.WARN : Theme.BRAND;
        int tx = x + 12, tw = w - 24;
        List<String> lines = n.body() == null ? List.of() : wrap(g, n.body(), tw);
        int h = 10 + 11 + lines.size() * 10 + 6 + 16 + 8;
        Widgets.card(g, x, y, w, h);
        g.rect(x, y, 3, h, accent);
        g.bold(g.clip(n.title(), tw), tx, y + 9, Theme.TEXT);
        int ly = y + 22;
        for (String l : lines) {
            g.text(l, tx, ly, Theme.MUTED);
            ly += 10;
        }
        int by = ly + 4, bx = tx;
        if (n.url() != null) {
            String open = Lang.tr("core.notice.open");
            int ow = g.width(open) + 18;
            if (Widgets.button(ui, "notice-open:" + n.id(), bx, by, ow, 16, open, Widgets.Style.PRIMARY)) Announcements.open(n);
            ui.tooltip("notice-url:" + n.id(), bx, by, ow, 16, n.url());
            bx += ow + 6;
        }
        String dismiss = Lang.tr("core.notice.dismiss");
        int dw = g.width(dismiss) + 18;
        DISMISS_BOUNDS.put(n.id(), new int[] {bx, by, dw, 16});
        if (Widgets.button(ui, "notice-dismiss:" + n.id(), bx, by, dw, 16, dismiss, Widgets.Style.SECONDARY)) Announcements.dismiss(n.id());
        return y + h;
    }

    private static List<String> wrap(Gfx g, String text, int w) {
        List<String> out = new java.util.ArrayList<>();
        for (String para : text.split("\n")) {
            if (para.isBlank()) continue;
            out.addAll(Ui.wrap(g, para, w));
        }
        return out;
    }
}

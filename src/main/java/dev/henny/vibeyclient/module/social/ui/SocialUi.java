package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import dev.henny.vibeyclient.ui.toast.Heads;
import net.minecraft.client.gui.components.EditBox;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** Drawing helpers shared by the social views (Vibe look: square, 1 px lines, purple/teal accents). */
public final class SocialUi {
    private SocialUi() {}

    public static final int ROW = 26;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DM_HM = DateTimeFormatter.ofPattern("dd.MM. HH:mm");

    public static int statusColor(String status) {
        if (status == null) return Theme.DIM;
        return switch (status) {
            case "online" -> Theme.OK;
            case "away" -> Theme.WARN;
            case "busy" -> Theme.DANGER;
            default -> Theme.DIM;
        };
    }

    /** Head with a status square in the bottom right corner (status null = none). */
    public static void head(Gfx g, String userId, String skinUrl, int x, int y, int size, String status) {
        Heads.draw(g, userId, skinUrl, x, y, size);
        if (status != null) {
            int d = Math.max(4, size / 4);
            int dx = x + size - d + 1;
            int dy = y + size - d + 1;
            g.rect(dx - 1, dy - 1, d + 2, d + 2, Theme.SURFACE);
            g.rect(dx, dy, d, d, statusColor(status));
        }
    }

    /** Small count badge, right-aligned at {@code right}; returns its width (0 if n == 0). */
    public static int badge(Gfx g, int n, int right, int y, int color) {
        if (n <= 0) return 0;
        String s = n > 99 ? "99+" : Integer.toString(n);
        int w = Math.max(10, g.width(s) + 6);
        g.rect(right - w, y, w, 10, color);
        g.textCenter(s, right - w / 2 + 1, y + 1, 0xFFFFFFFF);
        return w;
    }

    /** Section caption with a count, e.g. "ONLINE — 3". */
    public static int caption(Gfx g, String text, int x, int y, int w) {
        Widgets.caption(g, text, x, y);
        return y + 13;
    }

    public static void separator(Gfx g, int x, int y, int w) {
        g.hline(x, y, w, Theme.LINE);
    }

    /** Hover background of a clickable row; true when clicked. Draw inner controls first. */
    public static boolean row(Ui ui, Object key, int x, int y, int w, int h, boolean selected) {
        boolean clicked = ui.clicked(x, y, w, h);
        float hov = ui.anim(key, 0, 0f).to(selected ? 1f : ui.hover(x, y, w, h) ? 0.6f : 0f);
        if (hov > 0f) ui.g.rect(x, y, w, h, Theme.alpha(Theme.SURFACE3, hov));
        if (selected) ui.g.rect(x, y, 2, h, Theme.BRAND);
        return clicked;
    }

    /** Text field with a placeholder (drawn while empty and not focused). */
    public static EditBox field(Ui ui, Object key, int x, int y, int w, String value, int max, String placeholder, Consumer<String> onChange) {
        EditBox box = Widgets.textField(ui, key, x, y, w, value, max, onChange);
        if (value.isEmpty() && !box.isFocused() && placeholder != null) {
            ui.g.text(ui.g.clip(placeholder, w - 10), x + 5, y + 4, Theme.DIM);
        }
        return box;
    }

    /** Compact button sized to its label. Returns true when clicked. */
    public static boolean smallButton(Ui ui, Object key, int x, int y, String label, Widgets.Style style) {
        int w = ui.g.width(label) + 12;
        return Widgets.button(ui, key, x, y, w, 14, label, style);
    }

    public static int smallButtonWidth(Gfx g, String label) {
        return g.width(label) + 12;
    }

    /** Right-aligned compact button; returns the left x it used (for the next one). */
    public static int smallButtonRight(Ui ui, Object key, int right, int y, String label, Widgets.Style style, Runnable onClick) {
        int w = smallButtonWidth(ui.g, label);
        if (Widgets.button(ui, key, right - w, y, w, 14, label, style) && onClick != null) onClick.run();
        return right - w - 4;
    }

    /** Back arrow + title header of a detail page; true when back was clicked. */
    public static boolean backHeader(Ui ui, Object key, int x, int y, int w, String title, boolean showBack) {
        Gfx g = ui.g;
        int tx = x + 10;
        boolean back = false;
        if (showBack) {
            boolean h = ui.hover(x + 4, y + 4, 18, 18);
            if (ui.clicked(x + 4, y + 4, 18, 18)) back = true;
            if (h) g.rect(x + 4, y + 4, 18, 18, Theme.SURFACE3);
            Widgets.arrow(g, x + 10, y + 9, false, h ? Theme.TEXT : Theme.MUTED);
            tx = x + 26;
        }
        g.text(g.clip(title, w - (tx - x) - 8), tx, y + 9, Theme.TEXT);
        g.hline(x, y + 25, w, Theme.LINE);
        return back;
    }

    public static void empty(Gfx g, String text, int x, int y, int w) {
        g.textCenter(g.clip(text, w - 16), x + w / 2, y, Theme.DIM);
    }

    // ============================================================================================ time

    public static String time(long epochMs) {
        if (epochMs <= 0) return "";
        var t = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault());
        if (t.toLocalDate().equals(LocalDate.now())) return HM.format(t);
        return DM_HM.format(t);
    }

    public static String date(long epochMs) {
        if (epochMs <= 0) return "";
        return DMY.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
    }

    /** "5 min", "2 h", "3 d" ago. */
    public static String ago(long epochMs) {
        if (epochMs <= 0) return "";
        long s = Math.max(0, (System.currentTimeMillis() - epochMs) / 1000);
        if (s < 60) return Lang.tr("social.time.now");
        if (s < 3600) return Lang.tr("social.time.minutes", s / 60);
        if (s < 86400) return Lang.tr("social.time.hours", s / 3600);
        return Lang.tr("social.time.days", s / 86400);
    }
}

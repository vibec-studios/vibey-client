package dev.henny.vibeyclient.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Disabled by this server" (PLAY-LINKS.md §5) in the settings UI – the one badge component. Settings opt in with
 * {@code .serverRule("zoom")} (the settings page then dims the row and shows this badge instead of the control, or
 * once in the card title when the whole card is blocked), modules list their keys in
 * {@link VibeyModule#serverRuleKeys()} (tile line and a notice on the page), custom panels call {@link #draw} or
 * {@link #drawRight} themselves. Everything is cheap enough for every frame.
 */
public final class ServerBadge {
    private ServerBadge() {}

    public static final int H = 12;

    /** True if the current server disabled this key. */
    public static boolean disabled(String key) {
        return key != null && ServerRules.isDisabled(key);
    }

    /** Short label: "Disabled by this server". */
    public static String label() {
        return Lang.tr("core.settings.serverDisabled");
    }

    /** Badge width for {@link #label()}. */
    public static int width(Gfx g) {
        return g.width(label()) + 8;
    }

    /** Tooltip: what it means, plus the server's own message. */
    public static String hint() {
        String m = ServerRules.message();
        String help = Lang.tr("core.settings.serverDisabledHelp");
        return m == null ? help : help + "\n\"" + m + "\"";
    }

    /** Stable tooltip keys per owner (tooltips compare keys by identity). */
    private static final Map<Object, Object> KEYS = new IdentityHashMap<>();

    /**
     * Draws the badge with its top-left at (x, y), {@link #H} high, with a tooltip. {@code owner} is any stable
     * object of the caller (the setting, the module). Returns its width.
     */
    public static int draw(Ui ui, Object owner, int x, int y) {
        int w = Widgets.chip(ui.g, label(), x, y, Theme.WARN);
        ui.tooltip(KEYS.computeIfAbsent(owner, k -> new Object()), x, y, w, H, hint());
        return w;
    }

    /** Same, right-aligned so it ends at {@code right}. Returns its left x. */
    public static int drawRight(Ui ui, Object owner, int right, int y) {
        int x = right - width(ui.g);
        draw(ui, owner, x, y);
        return x;
    }

    /** The module's keys the current server disabled (empty almost always). */
    public static List<String> disabledKeys(VibeyModule m) {
        List<String> keys = m.serverRuleKeys();
        if (keys.isEmpty() || ServerRules.active().isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (String k : keys) if (ServerRules.isDisabled(k)) out.add(k);
        return out;
    }

    /**
     * Notice block on a module page ("Disabled by this server: emotes" plus the server message), like the module's
     * other notices. Returns the y below it (unchanged when nothing is disabled).
     */
    public static int notice(Ui ui, VibeyModule m, int x, int y, int w) {
        List<String> keys = disabledKeys(m);
        if (keys.isEmpty()) return y;
        Gfx g = ui.g;
        String text = Lang.tr("serverrules.notice", String.join(", ", keys));
        String msg = ServerRules.message();
        List<String> lines = new ArrayList<>(Ui.wrap(g, text, w - 16));
        if (msg != null) lines.addAll(Ui.wrap(g, "\"" + msg + "\"", w - 16));
        int h = lines.size() * 10 + 10;
        g.rect(x, y, w, h, Theme.alpha(Theme.WARN, 0.10f));
        g.vline(x, y, h, Theme.WARN);
        int ly = y + 6;
        for (int i = 0; i < lines.size(); i++) {
            g.text(lines.get(i), x + 8, ly, i == 0 ? Theme.WARN : Theme.MUTED);
            ly += 10;
        }
        return y + h + 10;
    }
}

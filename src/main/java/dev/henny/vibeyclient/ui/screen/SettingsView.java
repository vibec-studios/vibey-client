package dev.henny.vibeyclient.ui.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.ColorSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.ServerBadge;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.Widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Renders the settings of a {@link SettingsGroup} as a clean page: the settings grouped into cards by
 * {@link Setting#section} (switches without a description side by side in two columns), the keys in their own
 * "Keys" card, and every {@link Setting#advanced() advanced} setting in a collapsed "Advanced · for creators" area at
 * the bottom. Buttons ({@link ActionSetting}s that are not advanced) are not rows: the page header shows them
 * ({@link #actions}). Used for every module page and the General tab.
 */
public final class SettingsView {
    private static final int SLIDER_W = 96;
    private static final int VALUE_W = 34;
    private static final int CONTROL_W = SLIDER_W + VALUE_W + 6;
    private static final int ROW_PAD = 10;

    /** Groups whose advanced area is open (per game session). */
    private static final Set<SettingsGroup> OPEN = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<SettingsGroup, Object> ADV_KEYS = new IdentityHashMap<>();

    private SettingsView() {}

    /** Draws the whole page body from {@code y}, returns the y below it. */
    public static int draw(Ui ui, SettingsGroup group, int x, int y, int w, boolean enabled) {
        y = drawBasic(ui, group, x, y, w, enabled);
        return drawAdvanced(ui, group, x, y, w, enabled, null);
    }

    /** Buttons for the page header: visible, non-advanced {@link ActionSetting}s. */
    public static List<ActionSetting> actions(SettingsGroup group) {
        List<ActionSetting> out = new ArrayList<>();
        for (Setting<?, ?> s : group.all()) {
            if (s instanceof ActionSetting a && !s.isAdvanced() && s.isVisible()) out.add(a);
        }
        return out;
    }

    /** Label of an action as a header button: {@code <label>.action}, else the setting's label ("Open wardrobe"). */
    public static String actionLabel(ActionSetting a) {
        return Lang.has(a.nameKey() + ".action") ? Lang.tr(a.nameKey() + ".action") : Lang.tr(a.nameKey());
    }

    /** Label of an action as a button in a row (next to its label): {@code <label>.button}, else the label. */
    public static String buttonLabel(ActionSetting a) {
        return Lang.has(a.nameKey() + ".button") ? Lang.tr(a.nameKey() + ".button") : Lang.tr(a.nameKey());
    }

    // ============================================================================================ basic

    /** Sections and keys (everything a normal player sees). */
    public static int drawBasic(Ui ui, SettingsGroup group, int x, int y, int w, boolean enabled) {
        return drawBasic(ui, group, x, y, w, enabled, null, null);
    }

    /** Like {@link #drawBasic(Ui, SettingsGroup, int, int, int, boolean)}; {@code after} draws extra content right below section {@code sectionKey}. */
    public static int drawBasic(Ui ui, SettingsGroup group, int x, int y, int w, boolean enabled, String sectionKey, Extra after) {
        Map<String, List<Setting<?, ?>>> sections = new LinkedHashMap<>();
        List<Setting<?, ?>> keys = new ArrayList<>();
        for (Setting<?, ?> s : group.all()) {
            if (s.isAdvanced() || !s.isVisible() || s instanceof ActionSetting) continue;
            if (s instanceof KeybindSetting) keys.add(s);
            else sections.computeIfAbsent(s.section() == null ? "" : s.section(), k -> new ArrayList<>()).add(s);
        }
        boolean first = true;
        for (Map.Entry<String, List<Setting<?, ?>>> e : sections.entrySet()) {
            if (!first) y += 10;
            first = false;
            String title = e.getKey().isEmpty() ? null : Lang.tr(e.getValue().get(0).sectionNameKey());
            y = card(ui, title, e.getValue(), x, y, w, enabled);
            if (after != null && e.getKey().equals(sectionKey)) y = after.draw(ui, x, y, w);
        }
        if (!keys.isEmpty()) {
            if (!first) y += 10;
            first = false;
            y = card(ui, Lang.tr("core.settings.keys"), keys, x, y, w, enabled);
        }
        if (first && !hasAdvanced(group)) {
            Widgets.card(ui.g, x, y, w, 30);
            ui.g.textCenter(Lang.tr("core.settings.none"), x + w / 2, y + 11, Theme.DIM);
            y += 30;
        }
        return y;
    }

    // ============================================================================================ advanced

    public static boolean hasAdvanced(SettingsGroup group) {
        for (Setting<?, ?> s : group.all()) if (s.isAdvanced() && s.isVisible()) return true;
        return false;
    }

    public static boolean advancedOpen(SettingsGroup group) {
        return OPEN.contains(group);
    }

    public static void setAdvancedOpen(SettingsGroup group, boolean open) {
        if (open) OPEN.add(group);
        else OPEN.remove(group);
    }

    /**
     * The collapsible "Advanced · for creators" area. {@code extra} (may be null) draws more rows into it when open
     * (e.g. technical info): it gets (x, y, w) and returns the new y. Draws nothing without advanced settings and
     * without {@code extra}.
     */
    public static int drawAdvanced(Ui ui, SettingsGroup group, int x, int y, int w, boolean enabled, Extra extra) {
        List<Setting<?, ?>> adv = new ArrayList<>();
        for (Setting<?, ?> s : group.all()) if (s.isAdvanced() && s.isVisible()) adv.add(s);
        if (adv.isEmpty() && extra == null) return y;
        y += 14;
        boolean open = OPEN.contains(group);
        Object key = ADV_KEYS.computeIfAbsent(group, k -> new Object());
        String hint = Lang.tr(open ? "core.settings.advancedHide" : "core.settings.advancedHint");
        if (Widgets.disclosure(ui, key, x, y, w, 22, Lang.tr("core.settings.advanced"), hint, open)) {
            setAdvancedOpen(group, !open);
        }
        y += 22;
        if (!open) return y;
        y += 6;
        if (!adv.isEmpty()) y = card(ui, null, adv, x, y, w, enabled);
        if (extra != null) y = extra.draw(ui, x, y + (adv.isEmpty() ? 0 : 8), w);
        return y;
    }

    /** Extra content of the advanced area. */
    public interface Extra {
        int draw(Ui ui, int x, int y, int w);
    }

    // ============================================================================================ cards and rows

    private static int card(Ui ui, String title, List<Setting<?, ?>> list, int x, int y, int w, boolean enabled) {
        Gfx g = ui.g;
        boolean allBlocked = !list.isEmpty();
        for (Setting<?, ?> s : list) allBlocked &= s.blockedByServer();
        boolean rowBadge = !(allBlocked && title != null);
        if (title != null) {
            Widgets.caption(g, title, x + 1, y);
            if (allBlocked) ServerBadge.drawRight(ui, list.get(0), x + w, y - 2);
            y += 12;
        }
        int h = measure(g, list, w);
        if (ui.visibleY(y, h)) Widgets.card(g, x, y, w, h);
        int cy = y;
        int i = 0;
        boolean firstRow = true;
        while (i < list.size()) {
            int run = plainBoolRun(g, list, i);
            if (!firstRow) g.hline(x + 1, cy, w - 2, Theme.LINE);
            firstRow = false;
            if (run >= 3 && w >= 300) {
                int colW = w / 2;
                for (int k = 0; k < run; k += 2) {
                    if (k > 0) g.hline(x + 1, cy, w - 2, Theme.LINE);
                    boolRow(ui, (BoolSetting) list.get(i + k), x, cy, colW, 24, enabled, rowBadge);
                    if (k + 1 < run) {
                        g.vline(x + colW, cy + 4, 16, Theme.LINE);
                        boolRow(ui, (BoolSetting) list.get(i + k + 1), x + colW, cy, w - colW, 24, enabled, rowBadge);
                    }
                    cy += 24;
                }
                i += run;
            } else {
                cy = row(ui, list.get(i), x, cy, w, enabled, rowBadge);
                i++;
            }
        }
        return y + h;
    }

    /** Number of consecutive switches without a description starting at {@code i}. */
    private static int plainBoolRun(Gfx g, List<Setting<?, ?>> list, int i) {
        int n = 0;
        while (i + n < list.size() && list.get(i + n) instanceof BoolSetting b && !Lang.has(b.descriptionKey())) n++;
        return n;
    }

    private static int measure(Gfx g, List<Setting<?, ?>> list, int w) {
        int h = 0;
        int i = 0;
        while (i < list.size()) {
            int run = plainBoolRun(g, list, i);
            if (run >= 3 && w >= 300) {
                h += ((run + 1) / 2) * 24;
                i += run;
            } else {
                h += rowHeight(g, list.get(i), w);
                i++;
            }
        }
        return h;
    }

    private static List<String> descLines(Gfx g, Setting<?, ?> s, int labelW) {
        if (!Lang.has(s.descriptionKey())) return List.of();
        List<String> lines = Ui.wrap(g, Lang.tr(s.descriptionKey()), labelW);
        if (lines.size() > 2) {
            lines = new ArrayList<>(lines.subList(0, 2));
            lines.set(1, g.clip(lines.get(1) + " …", labelW));
        }
        return lines;
    }

    private static int labelWidth(Gfx g, Setting<?, ?> s, int w) {
        return w - ROW_PAD * 2 - controlWidth(g, s, w) - 12;
    }

    private static int rowHeight(Gfx g, Setting<?, ?> s, int w) {
        int n = descLines(g, s, labelWidth(g, s, w)).size();
        return n == 0 ? 26 : 22 + n * 10;
    }

    /** A switch cell of the two-column grid: the whole cell is the click target. */
    private static void boolRow(Ui ui, BoolSetting b, int x, int y, int w, int h, boolean enabled, boolean rowBadge) {
        Gfx g = ui.g;
        if (b.blockedByServer()) {
            blockedRow(ui, b, x, y, w, h, rowBadge);
            return;
        }
        boolean clicked = ui.clicked(x + 1, y, w - 1, h);
        boolean hot = ui.focusedLast() || ui.hover(x + 1, y, w - 1, h);
        float hov = ui.anim(b, 0f).to(hot ? 1f : 0f);
        if (hov > 0f) g.rect(x + 1, y + 1, w - 2, h - 1, Theme.alpha(Theme.SURFACE3, hov));
        int tx = x + w - ROW_PAD - Widgets.TOGGLE_W;
        String name = Lang.tr(b.nameKey());
        int lw = tx - x - ROW_PAD - 6;
        g.text(g.clip(name, lw), x + ROW_PAD, y + (h - 8) / 2, enabled ? Theme.TEXT : Theme.MUTED);
        if (g.width(name) > lw) ui.tooltip(b.nameKey(), x, y, w, h, name);
        Widgets.drawToggle(ui, b, tx, y + (h - Widgets.TOGGLE_H) / 2, b.on(), true, hot);
        if (clicked) b.toggle();
    }

    private static int row(Ui ui, Setting<?, ?> s, int x, int y, int w, boolean enabled, boolean rowBadge) {
        Gfx g = ui.g;
        int labelW = labelWidth(g, s, w);
        List<String> desc = descLines(g, s, labelW);
        int h = desc.isEmpty() ? 26 : 22 + desc.size() * 10;
        if (!ui.visibleY(y, h)) return y + h;
        if (s.blockedByServer()) {
            blockedRow(ui, s, x, y, w, h, rowBadge);
            return y + h;
        }
        boolean whole = s instanceof BoolSetting; // a switch row toggles anywhere
        boolean clicked = whole && ui.clicked(x + 1, y, w - 2, h);
        boolean hot = (whole && ui.focusedLast()) || ui.hover(x + 1, y, w - 2, h);
        float hov = ui.anim(s, 0f).to(hot ? 1f : 0f);
        if (hov > 0f) g.rect(x + 1, y + 1, w - 2, h - 1, Theme.alpha(Theme.SURFACE3, hov * (whole ? 1f : 0.6f)));
        int tx = x + ROW_PAD;
        String name = Lang.tr(s.nameKey());
        int textColor = enabled ? Theme.TEXT : Theme.MUTED;
        if (desc.isEmpty()) {
            g.text(g.clip(name, labelW), tx, y + 9, textColor);
        } else {
            g.text(g.clip(name, labelW), tx, y + 6, textColor);
            int dy = y + 17;
            for (String l : desc) {
                g.text(g.clip(l, labelW), tx, dy, Theme.DIM);
                dy += 10;
            }
        }
        String full = Lang.has(s.descriptionKey()) ? Lang.tr(s.descriptionKey()) : null;
        if (g.width(name) > labelW || (full != null && Ui.wrap(g, full, labelW).size() > 2)) {
            ui.tooltip(s.nameKey(), tx, y, labelW, h, full == null ? name : name + "\n" + full);
        }
        int right = x + w - ROW_PAD;
        int cy = y + h / 2;
        if (s instanceof BoolSetting b) {
            Widgets.drawToggle(ui, b, right - Widgets.TOGGLE_W, cy - Widgets.TOGGLE_H / 2, b.on(), true, hot);
            if (clicked) b.toggle();
        } else {
            control(ui, s, right, cy, w);
        }
        return y + h;
    }

    /**
     * A row whose feature the current server disables ({@link Setting#serverRule}): dimmed label, no control, and the
     * "Disabled by this server" badge in place of the control (unless the card title already shows it).
     */
    private static void blockedRow(Ui ui, Setting<?, ?> s, int x, int y, int w, int h, boolean badge) {
        Gfx g = ui.g;
        int right = x + w - ROW_PAD;
        int chipW = badge ? ServerBadge.width(g) : 0;
        int labelW = right - (x + ROW_PAD) - chipW - 8;
        g.text(g.clip(Lang.tr(s.nameKey()), labelW), x + ROW_PAD, y + (h - 8) / 2, Theme.DIM);
        if (badge) ServerBadge.drawRight(ui, s, right, y + (h - ServerBadge.H) / 2);
    }

    private static int controlWidth(Gfx g, Setting<?, ?> s, int rowW) {
        if (s instanceof BoolSetting) return Widgets.TOGGLE_W;
        if (s instanceof IntSetting i && i.isField()) return 70;
        if (s instanceof StringSetting) return Math.min(200, Math.max(120, rowW / 2 - 20));
        if (s instanceof ColorSetting c) return Widgets.swatchesWidth(c.presets().length);
        if (s instanceof KeybindSetting) return 96;
        if (s instanceof ActionSetting a) return Math.max(60, g.width(buttonLabel(a)) + 16);
        if (s instanceof EnumSetting<?> e) return enumWidth(g, e, rowW);
        return CONTROL_W;
    }

    private static <E extends Enum<E>> String[] enumLabels(EnumSetting<E> e) {
        E[] values = e.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = Lang.tr(e.valueKey(values[i]));
        return labels;
    }

    /** Segmented when the options fit in about half the row, else a ‹ value › cycler. */
    private static int enumWidth(Gfx g, EnumSetting<?> e, int rowW) {
        int seg = Widgets.segmentedWidth(g, enumLabels(e));
        if (e.values().length <= 4 && seg <= Math.max(CONTROL_W, rowW / 2 - 10)) return Math.max(seg, CONTROL_W);
        return CONTROL_W;
    }

    private static void control(Ui ui, Setting<?, ?> s, int right, int cy, int rowW) {
        Gfx g = ui.g;
        if (s instanceof IntSetting i && i.isField()) {
            Widgets.textField(ui, i, right - 70, cy - 8, 70, String.valueOf(i.get()), 11, text -> {
                try {
                    int v = Integer.parseInt(text.trim());
                    if (v >= i.min() && v <= i.max()) i.set(v);
                } catch (NumberFormatException ignored) {
                    // keep the old value until the text is a valid number
                }
            });
        } else if (s instanceof IntSetting i) {
            g.textRight(i.get() + i.unit(), right, cy - 4, Theme.MUTED);
            float f = (i.get() - i.min()) / (float) Math.max(1, i.max() - i.min());
            float nf = Widgets.slider(ui, i, right - CONTROL_W, cy - 6, SLIDER_W, f);
            if (nf != f) {
                int step = Math.max(1, i.step());
                int v = i.min() + Math.round(nf * (i.max() - i.min()) / step) * step;
                i.set(v);
            }
        } else if (s instanceof FloatSetting fs) {
            boolean percent = fs.min() == 0f && fs.max() == 1f;
            String v = percent ? Math.round(fs.get() * 100) + "%" : String.format(Locale.ROOT, "%." + fs.decimals() + "f", fs.get());
            g.textRight(v, right, cy - 4, Theme.MUTED);
            float range = Math.max(1e-6f, fs.max() - fs.min());
            float f = (fs.get() - fs.min()) / range;
            float nf = Widgets.slider(ui, fs, right - CONTROL_W, cy - 6, SLIDER_W, f);
            if (nf != f) {
                float step = fs.step() > 0 ? fs.step() : 0.01f;
                fs.set(fs.min() + Math.round(nf * range / step) * step);
            }
        } else if (s instanceof EnumSetting<?> e) {
            enumControl(ui, e, right, cy, rowW);
        } else if (s instanceof StringSetting str) {
            int fw = controlWidth(g, s, rowW);
            Widgets.textField(ui, str, right - fw, cy - 8, fw, str.get(), str.maxLength(), str::set);
        } else if (s instanceof ColorSetting c) {
            int[] presets = c.presets();
            int picked = Widgets.swatches(ui, right - Widgets.swatchesWidth(presets.length), cy - 6, presets, c.argb());
            if (picked != 0) c.set(picked);
        } else if (s instanceof KeybindSetting k) {
            keybind(ui, k, right, cy);
        } else if (s instanceof ActionSetting a) {
            String label = buttonLabel(a);
            int bw = Math.max(60, g.width(label) + 16);
            if (Widgets.button(ui, a, right - bw, cy - 8, bw, 16, label, Widgets.Style.SECONDARY)) a.run();
        }
    }

    private static <E extends Enum<E>> void enumControl(Ui ui, EnumSetting<E> e, int right, int cy, int rowW) {
        String[] labels = enumLabels(e);
        int w = enumWidth(ui.g, e, rowW);
        if (e.values().length <= 4 && Widgets.segmentedWidth(ui.g, labels) <= w) {
            int picked = Widgets.segmented(ui, e, right - w, cy - 8, w, labels, e.get().ordinal());
            if (picked >= 0) e.set(e.values()[picked]);
        } else {
            int d = Widgets.cycler(ui, e, right - CONTROL_W, cy - 8, CONTROL_W, Lang.tr(e.valueKey(e.get())));
            if (d != 0) e.cycle(d);
        }
    }

    private static void keybind(Ui ui, KeybindSetting k, int right, int cy) {
        Gfx g = ui.g;
        boolean waiting = ui.capturing() == captureOf(k);
        boolean unbound = ClientCompat.isUnbound(k.mapping());
        String label = waiting ? Lang.tr("core.settings.pressKey") : unbound ? Lang.tr("core.settings.unbound")
                : ClientCompat.keyName(k.mapping()).getString();
        List<net.minecraft.client.KeyMapping> conflicts = waiting ? List.of() : ClientCompat.conflicts(k.mapping());
        int bw = 96, bx = right - bw, by = cy - 8;
        if (Widgets.keycap(ui, k, bx, by, bw, label, waiting, !conflicts.isEmpty())) ui.capture(captureOf(k));
        if (!conflicts.isEmpty()) {
            Icons.draw(g, Icons.WARN, bx - 14, by + 4, Theme.WARN);
            StringBuilder names = new StringBuilder();
            for (net.minecraft.client.KeyMapping m : conflicts) {
                if (names.length() > 0) names.append(", ");
                names.append(ClientCompat.mappingName(m));
            }
            ui.tooltip(k, bx - 16, by, bw + 16, 16, Lang.tr("core.settings.conflict", names.toString()));
        } else {
            ui.tooltip(k, bx, by, bw, 16, Lang.tr("core.settings.keyHelp"));
        }
    }

    // ============================================================================================ key capture

    /** Escape cancels, Backspace/Delete unbinds, anything else binds. */
    static void applyKey(KeybindSetting k, int keyCode) {
        if (keyCode == Keys.ESCAPE) return;
        if (keyCode == Keys.BACKSPACE || keyCode == Keys.DELETE) keyCode = -1;
        ClientCompat.rebind(k.mapping(), keyCode < 0 ? -1 : Keys.toNativeAny(keyCode));
    }

    private static final Map<KeybindSetting, VibeyScreen.KeyCapture> CAPTURES = new IdentityHashMap<>();

    /** Capture object of a keybind setting (stable per setting). */
    public static VibeyScreen.KeyCapture captureOf(KeybindSetting k) {
        return CAPTURES.computeIfAbsent(k, key -> code -> applyKey(key, code));
    }

    /** Search: does a visible setting label of the group contain {@code q} (lower case)? Returns its label or null. */
    public static String matchingSetting(SettingsGroup group, String q) {
        if (q.isEmpty()) return null;
        for (Setting<?, ?> s : group.all()) {
            if (!s.isVisible()) continue;
            String name = Lang.tr(s.nameKey());
            if (name.toLowerCase(Locale.ROOT).contains(q)) return name;
        }
        return null;
    }
}

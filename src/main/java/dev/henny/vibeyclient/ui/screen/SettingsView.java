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
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.Widgets;
import org.lwjgl.glfw.GLFW;

/**
 * Renders the settings of a {@link SettingsGroup} automatically: one row per visible setting (label + optional
 * description on the left, control on the right), advanced ones under a separate caption. Used for modules
 * without a custom panel and for the global settings.
 */
public final class SettingsView {
    private static final int CONTROL_W = 120;

    private SettingsView() {}

    /** Draws all rows starting at {@code y}, returns the y below the last one. */
    public static int draw(Ui ui, SettingsGroup group, int x, int y, int w, boolean enabled) {
        boolean any = false;
        for (Setting<?, ?> s : group.all()) {
            if (s.isAdvanced() || !s.isVisible()) continue;
            y = row(ui, s, x, y, w, enabled);
            any = true;
        }
        boolean header = false;
        for (Setting<?, ?> s : group.all()) {
            if (!s.isAdvanced() || !s.isVisible()) continue;
            if (!header) {
                y += 8;
                Widgets.caption(ui.g, Lang.tr("core.settings.advanced"), x, y);
                ui.g.hline(x + ui.g.width(Lang.tr("core.settings.advanced").toUpperCase(java.util.Locale.ROOT)) + 6, y + 4, w - ui.g.width(Lang.tr("core.settings.advanced").toUpperCase(java.util.Locale.ROOT)) - 6, Theme.LINE);
                y += 14;
                header = true;
            }
            y = row(ui, s, x, y, w, enabled);
            any = true;
        }
        if (!any) {
            ui.g.rect(x, y, w, 28, Theme.SURFACE2);
            ui.g.textCenter(Lang.tr("core.settings.none"), x + w / 2, y + 10, Theme.DIM);
            y += 28;
        }
        return y;
    }

    private static int row(Ui ui, Setting<?, ?> s, int x, int y, int w, boolean enabled) {
        Gfx g = ui.g;
        String desc = Lang.has(s.descriptionKey()) ? Lang.tr(s.descriptionKey()) : null;
        int h = desc == null ? 26 : 34;
        if (!ui.visibleY(y, h)) return y + h;
        float hov = ui.anim(s, 0f).to(ui.hover(x, y, w, h) ? 1f : 0f);
        if (hov > 0f) g.rect(x - 4, y, w + 8, h, Theme.alpha(Theme.SURFACE2, hov));
        int labelW = w - CONTROL_W - 16;
        int textColor = enabled ? Theme.TEXT : Theme.MUTED;
        if (desc == null) {
            g.text(g.clip(Lang.tr(s.nameKey()), labelW), x, y + 9, textColor);
        } else {
            g.text(g.clip(Lang.tr(s.nameKey()), labelW), x, y + 6, textColor);
            g.text(g.clip(desc, labelW), x, y + 18, Theme.DIM);
        }
        g.hline(x, y + h - 1, w, Theme.LINE);
        int right = x + w;
        int cy = y + h / 2;
        control(ui, s, right, cy);
        return y + h;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void control(Ui ui, Setting<?, ?> s, int right, int cy) {
        Gfx g = ui.g;
        if (s instanceof BoolSetting b) {
            if (Widgets.toggle(ui, b, right - Widgets.TOGGLE_W, cy - Widgets.TOGGLE_H / 2, b.on(), true)) b.toggle();
        } else if (s instanceof IntSetting i) {
            int sw = CONTROL_W - 36;
            String v = i.get() + i.unit();
            g.textRight(v, right, cy - 4, Theme.MUTED);
            int sx = right - CONTROL_W;
            float f = (i.get() - i.min()) / (float) Math.max(1, i.max() - i.min());
            float nf = Widgets.slider(ui, i, sx, cy - 6, sw, f);
            if (nf != f) i.set(Math.round(i.min() + nf * (i.max() - i.min())));
        } else if (s instanceof FloatSetting fs) {
            int sw = CONTROL_W - 36;
            String v = String.format(java.util.Locale.ROOT, "%." + fs.decimals() + "f", fs.get());
            g.textRight(v, right, cy - 4, Theme.MUTED);
            int sx = right - CONTROL_W;
            float range = Math.max(1e-6f, fs.max() - fs.min());
            float f = (fs.get() - fs.min()) / range;
            float nf = Widgets.slider(ui, fs, sx, cy - 6, sw, f);
            if (nf != f) fs.set(fs.min() + nf * range);
        } else if (s instanceof EnumSetting<?> e) {
            enumControl(ui, e, right, cy);
        } else if (s instanceof StringSetting str) {
            Widgets.textField(ui, str, right - CONTROL_W - 40, cy - 8, CONTROL_W + 40, str.get(), str.maxLength(), str::set);
        } else if (s instanceof ColorSetting c) {
            int[] presets = c.presets();
            int picked = Widgets.swatches(ui, right - Widgets.swatchesWidth(presets.length), cy - 6, presets, c.argb());
            if (picked != 0) c.set(picked);
        } else if (s instanceof KeybindSetting k) {
            boolean waiting = ui.capturing() == captureOf(k);
            String label = waiting ? "> " + Lang.tr("core.settings.pressKey") + " <" : ClientCompat.keyName(k.mapping()).getString();
            if (Widgets.button(ui, k, right - 90, cy - 8, 90, 16, label, waiting ? Widgets.Style.PRIMARY : Widgets.Style.SECONDARY)) {
                ui.capture(captureOf(k));
            }
        } else if (s instanceof ActionSetting a) {
            String label = Lang.tr(a.nameKey() + ".button");
            if (!Lang.has(a.nameKey() + ".button")) label = Lang.tr("core.settings.run");
            int bw = Math.max(60, g.width(label) + 16);
            if (Widgets.button(ui, a, right - bw, cy - 8, bw, 16, label, Widgets.Style.SECONDARY)) a.run();
        }
    }

    private static <E extends Enum<E>> void enumControl(Ui ui, EnumSetting<E> e, int right, int cy) {
        int d = Widgets.cycler(ui, e, right - CONTROL_W, cy - 8, CONTROL_W, Lang.tr(e.valueKey(e.get())));
        if (d != 0) e.cycle(d);
    }

    /** Escape cancels, Backspace/Delete unbinds, anything else binds. */
    static void applyKey(KeybindSetting k, int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) keyCode = -1;
        ClientCompat.rebind(k.mapping(), keyCode);
    }

    private static final java.util.Map<KeybindSetting, VibeyScreen.KeyCapture> CAPTURES = new java.util.IdentityHashMap<>();

    /** Capture object of a keybind setting (stable per setting). */
    public static VibeyScreen.KeyCapture captureOf(KeybindSetting k) {
        return CAPTURES.computeIfAbsent(k, key -> code -> applyKey(key, code));
    }
}

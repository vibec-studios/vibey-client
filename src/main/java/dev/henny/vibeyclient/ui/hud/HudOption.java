package dev.henny.vibeyclient.ui.hud;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A per-widget option shown in the style popover's "Widget" page (e.g. "Show biome", "Item: Arrows"). Values are
 * stored as strings in the widget's placement ({@link HudPaint#option(String)}, {@link HudPaint#flag(String)}), so
 * duplicates of a widget each keep their own. Labels are Vibey i18n keys.
 *
 * <pre>{@code
 * .options(HudOption.toggle("biome", "hud.opt.biome", true),
 *          HudOption.choice("format", "hud.opt.format", "24h", "24h", "12h"),   // value labels: hud.opt.format.24h, ...
 *          HudOption.choice("item", "hud.opt.item", "minecraft:arrow", ids...)
 *                  .display(id -> itemName(id))                            // literal value names instead of keys
 *                  .capture("hud.opt.item.held", () -> heldItemId()))       // "Use held item" button
 * }</pre>
 */
public final class HudOption {
    public enum Kind { TOGGLE, CHOICE }

    public final String key;
    public final String labelKey;
    public final Kind kind;
    public final String def;
    public final List<String> values;
    private Function<String, String> display;
    private String captureLabel;
    private Supplier<String> capture;

    private HudOption(String key, String labelKey, Kind kind, String def, List<String> values) {
        if (!key.matches("[a-z0-9_]{1,24}")) throw new IllegalArgumentException("Bad option key " + key);
        this.key = key;
        this.labelKey = labelKey;
        this.kind = kind;
        this.def = def;
        this.values = values;
    }

    public static HudOption toggle(String key, String labelKey, boolean def) {
        return new HudOption(key, labelKey, Kind.TOGGLE, def ? "1" : "0", List.of("0", "1"));
    }

    /** A ‹ value › cycler. Value names are {@code labelKey + "." + value} unless {@link #display} is set. */
    public static HudOption choice(String key, String labelKey, String def, String... values) {
        return new HudOption(key, labelKey, Kind.CHOICE, def, List.of(values));
    }

    /** Shows values with this function (literal text) instead of i18n keys. */
    public HudOption display(Function<String, String> f) {
        this.display = f;
        return this;
    }

    /** Adds a small button next to the cycler that sets the value from {@code value} (null result = ignored). */
    public HudOption capture(String labelKey, Supplier<String> value) {
        this.captureLabel = labelKey;
        this.capture = value;
        return this;
    }

    public String displayValue(String v) {
        if (display != null) {
            try {
                String s = display.apply(v);
                if (s != null) return s;
            } catch (RuntimeException ignored) {
                // fall through
            }
        }
        return dev.henny.vibeyclient.api.Lang.tr(labelKey + "." + v);
    }

    public String captureLabel() {
        return captureLabel;
    }

    public String captureValue() {
        if (capture == null) return null;
        try {
            String v = capture.get();
            return v == null || v.isBlank() || v.length() > 96 ? null : v;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Accepts a stored value: toggles 0/1, choices one of the values or (with a capture) any short id. */
    public boolean accepts(String v) {
        if (v == null) return false;
        if (kind == Kind.TOGGLE) return v.equals("0") || v.equals("1");
        return values.contains(v) || (capture != null && v.length() <= 96 && v.matches("[a-z0-9_.:/-]+"));
    }
}

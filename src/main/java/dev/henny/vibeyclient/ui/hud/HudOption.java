package dev.henny.vibeyclient.ui.hud;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

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
        if (!key.matches("[a-zA-Z0-9_]{1,24}")) throw new IllegalArgumentException("Bad option key " + key);
        this.key = key;
        this.labelKey = labelKey;
        this.kind = kind;
        this.def = def;
        this.values = values;
    }

    public static HudOption toggle(String key, String labelKey, boolean def) {
        return new HudOption(key, labelKey, Kind.TOGGLE, def ? "1" : "0", List.of("0", "1"));
    }

    public static HudOption choice(String key, String labelKey, String def, String... values) {
        return new HudOption(key, labelKey, Kind.CHOICE, def, List.of(values));
    }

    public HudOption display(Function<String, String> f) {
        this.display = f;
        return this;
    }

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

    public boolean accepts(String v) {
        if (v == null) return false;
        if (kind == Kind.TOGGLE) return v.equals("0") || v.equals("1");
        return values.contains(v) || (capture != null && v.length() <= 96 && v.matches("[a-z0-9_.:/-]+"));
    }
}

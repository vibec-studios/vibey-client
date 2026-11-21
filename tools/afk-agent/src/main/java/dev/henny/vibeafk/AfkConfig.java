package dev.henny.vibeafk;

import java.util.Map;

public final class AfkConfig {
    public static final AfkConfig FOREGROUND = new AfkConfig(null, false, 2, true, true, false, 5, 10, 300, 6, 6, null);

    public final String session;
    public final boolean background;
    public final int backgroundFps;
    public final boolean mute;
    public final boolean hideWindow;
    public final boolean autoReconnect;
    public final int maxAttempts;
    public final int baseDelayS;
    public final int maxDelayS;
    public final double lowHealth;
    public final int lowFood;
    public final String mappings;

    public AfkConfig(String session, boolean background, int backgroundFps, boolean mute, boolean hideWindow,
                     boolean autoReconnect, int maxAttempts, int baseDelayS, int maxDelayS,
                     double lowHealth, int lowFood, String mappings) {
        this.session = session;
        this.background = background;
        this.backgroundFps = backgroundFps;
        this.mute = mute;
        this.hideWindow = hideWindow;
        this.autoReconnect = autoReconnect;
        this.maxAttempts = maxAttempts;
        this.baseDelayS = baseDelayS;
        this.maxDelayS = maxDelayS;
        this.lowHealth = lowHealth;
        this.lowFood = lowFood;
        this.mappings = mappings;
    }

    @SuppressWarnings("unchecked")
    public static AfkConfig parse(String text) {
        Map<String, Object> o = Json.parseObject(text);
        Map<String, Object> rc = o.get("auto_reconnect") instanceof Map ? (Map<String, Object>) o.get("auto_reconnect") : null;
        Map<String, Object> al = o.get("alerts") instanceof Map ? (Map<String, Object>) o.get("alerts") : null;
        String mode = str(o.get("mode"));
        int base = clamp(num(rc == null ? null : rc.get("base_delay_s"), 10), 5, 600);
        int max = Math.max(base, clamp(num(rc == null ? null : rc.get("max_delay_s"), 300), 10, 3600));
        return new AfkConfig(
                clip(str(o.get("session")), 128),
                "background".equals(mode),
                clamp(num(o.get("background_fps"), 2), 1, 10),
                bool(o.get("mute"), true),
                bool(o.get("hide_window"), true),
                bool(rc == null ? null : rc.get("enabled"), false),
                clamp(num(rc == null ? null : rc.get("max_attempts"), 5), 1, 50),
                base,
                max,
                Math.max(0, Math.min(40, dbl(al == null ? null : al.get("low_health"), 6))),
                clamp(num(al == null ? null : al.get("low_food"), 6), 0, 20),
                str(o.get("mappings")));
    }

    public String mode() {
        return background ? "background" : "foreground";
    }

    private static String str(Object v) {
        if (!(v instanceof String)) return null;
        String s = ((String) v).trim();
        return s.isEmpty() ? null : s;
    }

    private static String clip(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static int num(Object v, int def) {
        return v instanceof Number ? (int) Math.round(((Number) v).doubleValue()) : def;
    }

    private static double dbl(Object v, double def) {
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    private static boolean bool(Object v, boolean def) {
        return v instanceof Boolean ? (Boolean) v : def;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

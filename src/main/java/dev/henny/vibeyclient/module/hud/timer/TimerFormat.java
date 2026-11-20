package dev.henny.vibeyclient.module.hud.timer;

import java.util.Locale;

final class TimerFormat {
    private TimerFormat() {}

    static final String[] LENGTHS = {"30s", "1m", "2m", "3m", "5m", "10m", "15m", "20m", "30m", "45m", "60m"};

    static long lengthMs(String v) {
        if (v == null || v.length() < 2) return 60_000L;
        try {
            long n = Long.parseLong(v.substring(0, v.length() - 1));
            return v.endsWith("s") ? n * 1000L : n * 60_000L;
        } catch (NumberFormatException e) {
            return 60_000L;
        }
    }

    static String display(String v) {
        return v;
    }

    static String precise(long ms) {
        long t = ms / 100;
        long tenths = t % 10;
        long s = t / 10 % 60;
        long m = t / 600;
        if (m >= 60) return String.format(Locale.ROOT, "%d:%02d:%02d.%d", m / 60, m % 60, s, tenths);
        return String.format(Locale.ROOT, "%d:%02d.%d", m, s, tenths);
    }

    static String clock(long ms) {
        long s = (ms + 999) / 1000;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }
}

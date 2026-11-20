package dev.henny.vibeyclient.module.chat.core;

public final class HistorySize {
    public static final int VANILLA = 100;
    public static final int MIN = 100;
    public static final int MAX = 2000;

    private HistorySize() {}

    public static int clamp(int lines) {
        return Math.max(MIN, Math.min(MAX, lines));
    }
}

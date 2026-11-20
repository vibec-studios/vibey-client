package dev.henny.vibeyclient.module.chat.core;

public final class SpamCollapse {
    public static final long WINDOW_MS = 60_000;
    public static final int MAX_COUNT = 999;

    private SpamCollapse() {}

    public static boolean repeats(ChatEntry newest, String plain, long now) {
        return newest != null && newest.key().equals(normalize(plain)) && now - newest.at() <= WINDOW_MS
                && newest.count() < MAX_COUNT;
    }

    public static String normalize(String plain) {
        return plain.strip().replaceAll("\\s+", " ");
    }

    public static String suffix(int count) {
        return count > 1 ? " ×" + count : "";
    }
}

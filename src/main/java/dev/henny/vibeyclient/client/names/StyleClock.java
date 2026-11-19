package dev.henny.vibeyclient.client.names;

public final class StyleClock {
    private StyleClock() {}

    public static final long BUCKET_MS = 50;
    private static final long START = System.currentTimeMillis() / BUCKET_MS;

    public static long bucket() {
        return System.currentTimeMillis() / BUCKET_MS - START;
    }

    public static double seconds(long bucket) {
        return bucket * (BUCKET_MS / 1000.0);
    }
}

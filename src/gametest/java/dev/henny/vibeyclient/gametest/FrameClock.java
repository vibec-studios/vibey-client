package dev.henny.vibeyclient.gametest;

import java.util.Arrays;

public final class FrameClock {
    private static long[] times;
    private static int count;
    private static long last;

    private FrameClock() {}

    public static void sample() {
        long[] t = times;
        if (t == null) return;
        long now = System.nanoTime();
        if (last != 0 && count < t.length) t[count++] = now - last;
        last = now;
    }

    static void start(int max) {
        count = 0;
        last = 0;
        times = new long[max];
    }

    static double[] stop() {
        long[] t = times;
        times = null;
        if (t == null || count < 20) return new double[] {-1, -1};
        long[] s = Arrays.copyOf(t, count);
        double avg = Arrays.stream(s).average().orElse(0) / 1e6;
        Arrays.sort(s);
        return new double[] {avg, s[(int) (s.length * 0.99)] / 1e6, count};
    }
}

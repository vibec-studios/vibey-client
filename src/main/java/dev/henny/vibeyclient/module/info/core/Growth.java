package dev.henny.vibeyclient.module.info.core;

public final class Growth {
    private Growth() {}

    public static int percent(int age, int maxAge) {
        if (maxAge <= 0) return 100;
        int a = Math.max(0, Math.min(age, maxAge));
        return Math.round(a * 100f / maxAge);
    }

    public static boolean mature(int age, int maxAge) {
        return age >= maxAge;
    }

    public static String label(int age, int maxAge) {
        return percent(age, maxAge) + "%";
    }
}

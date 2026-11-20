package dev.henny.vibeyclient.media.ffmpeg;

import java.util.Locale;

final class FfmpegPlatform {
    private FfmpegPlatform() {}

    static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    static String exe(String name) {
        return windows() ? name + ".exe" : name;
    }

    static String tag() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (arch.equals("amd64") || arch.equals("x86_64")) return "win64";
        if (arch.equals("aarch64") || arch.equals("arm64")) return "winarm64";
        return null;
    }
}

package dev.henny.vibeyclient.media.ffmpeg;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Ffmpeg {
    public static final String SOURCE = "github.com/BtbN/FFmpeg-Builds";
    public static final String LICENSE = "LGPL-2.1-or-later";
    private static final String[] UNIX_PATHS = {"/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg", "/opt/homebrew/bin/ffmpeg"};

    private static volatile String found;
    private static volatile boolean searched;
    public static volatile boolean hiddenForTest;

    private Ffmpeg() {}

    public static Path ownDir() {
        return FabricLoader.getInstance().getGameDir().resolve("vibeyclient").resolve("ffmpeg");
    }

    public static String find() {
        if (searched) return found;
        String f = own();
        if (f == null && !hiddenForTest) f = onPath();
        if (f == null && !hiddenForTest && !FfmpegPlatform.windows()) f = inUnixPaths();
        found = f;
        searched = true;
        return f;
    }

    public static boolean available() {
        return find() != null;
    }

    public static void refresh() {
        searched = false;
    }

    public static boolean downloadable() {
        return FfmpegPlatform.windows() && FfmpegPlatform.tag() != null;
    }

    private static String own() {
        Path p = ownDir().resolve("bin").resolve(FfmpegPlatform.exe("ffmpeg"));
        return Files.isRegularFile(p) ? p.toString() : null;
    }

    private static String onPath() {
        String path = System.getenv("PATH");
        if (path == null) return null;
        for (String dir : path.split(File.pathSeparator)) {
            if (dir.isBlank()) continue;
            try {
                Path p = Path.of(dir.trim()).resolve(FfmpegPlatform.exe("ffmpeg"));
                if (Files.isRegularFile(p) && Files.isExecutable(p)) return p.toString();
            } catch (RuntimeException ignored) {}
        }
        return null;
    }

    private static String inUnixPaths() {
        for (String p : UNIX_PATHS) if (Files.isExecutable(Path.of(p))) return p;
        return null;
    }
}

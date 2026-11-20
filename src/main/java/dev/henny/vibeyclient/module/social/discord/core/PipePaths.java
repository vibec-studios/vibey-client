package dev.henny.vibeyclient.module.social.discord.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PipePaths {
    public static final int SLOTS = 10;
    private static final String[] ENV = {"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"};
    private static final String[] SUBDIRS = {"", "app/com.discordapp.Discord/", "snap.discord/", ".flatpak/com.discordapp.Discord/xdg-run/"};

    private PipePaths() {}

    public static boolean windows(String osName) {
        return osName != null && osName.toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    public static List<String> windowsPipes() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) out.add("\\\\.\\pipe\\discord-ipc-" + i);
        return out;
    }

    public static List<String> unixSockets(Map<String, String> env) {
        Set<String> dirs = new LinkedHashSet<>();
        for (String k : ENV) {
            String v = env.get(k);
            if (v != null && !v.isBlank()) dirs.add(v.endsWith("/") ? v : v + "/");
        }
        dirs.add("/tmp/");
        List<String> out = new ArrayList<>();
        for (String d : dirs) {
            for (String sub : SUBDIRS) {
                for (int i = 0; i < SLOTS; i++) out.add(d + sub + "discord-ipc-" + i);
            }
        }
        return out;
    }
}

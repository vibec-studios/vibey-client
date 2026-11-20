package dev.henny.vibeyclient.module.screens.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Blocklist {
    static final List<String> PREFIXES = List.of("dev.henny.vui.", "dev.henny.r3.", "dev.henny.cosmetico.",
            "dev.henny.vibeyclient.");
    static final List<String> NAMES = List.of("ChatScreen", "InBedChatScreen", "LevelLoadingScreen", "ReceivingLevelScreen",
            "ProgressScreen", "ConnectScreen", "GenericMessageScreen", "GenericWaitingScreen",
            "WinScreen", "LoadingOverlay", "BookEditScreen", "SignEditScreen", "HangingSignEditScreen");

    private final List<String> extra;

    public Blocklist(String userList) {
        List<String> out = new ArrayList<>();
        if (userList != null) for (String s : userList.split("[,;\\s]+")) if (!s.isBlank()) out.add(s.trim());
        this.extra = List.copyOf(out);
    }

    public boolean blocked(String className) {
        if (className == null || className.isEmpty()) return true;
        for (String p : PREFIXES) if (className.startsWith(p)) return true;
        String simple = className.substring(className.lastIndexOf('.') + 1);
        if (NAMES.contains(simple)) return true;
        for (String e : extra) {
            if (e.equalsIgnoreCase(simple) || e.equals(className)) return true;
            if (e.endsWith("*") && className.toLowerCase(Locale.ROOT).startsWith(e.substring(0, e.length() - 1).toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}

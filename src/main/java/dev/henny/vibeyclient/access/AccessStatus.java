package dev.henny.vibeyclient.access;

import java.util.Locale;

public enum AccessStatus {
    NONE, WAITLIST, APPROVED, REJECTED;

    public static AccessStatus parse(String s) {
        if (s == null) return NONE;
        return switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "waitlist" -> WAITLIST;
            case "approved" -> APPROVED;
            case "rejected" -> REJECTED;
            default -> NONE;
        };
    }

    public static boolean isDenial(String code) {
        return code != null && code.trim().toUpperCase(Locale.ROOT).startsWith("ACCESS_");
    }

    public static AccessStatus fromDenial(String code) {
        if (code == null) return NONE;
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "ACCESS_PENDING" -> WAITLIST;
            case "ACCESS_REJECTED" -> REJECTED;
            default -> NONE;
        };
    }
}

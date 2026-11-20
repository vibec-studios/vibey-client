package dev.henny.vibeyclient.module.social.presence;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalAddress {
    private static final String[] LOCAL_SUFFIXES = {".local", ".localhost", ".localdomain", ".lan", ".home", ".home.arpa",
            ".internal", ".intranet", ".private"};
    private static final String[] IP_DNS = {".nip.io", ".sslip.io", ".traefik.me", ".localtest.me"};
    private static final Pattern EMBEDDED = Pattern.compile("(?:^|[.-])(\\d{1,3})[.-](\\d{1,3})[.-](\\d{1,3})[.-](\\d{1,3})$");

    private LocalAddress() {}

    public static boolean isLocal(String host) {
        if (host == null) return true;
        String h = host.trim().toLowerCase(Locale.ROOT);
        if (h.startsWith("[") && h.endsWith("]")) h = h.substring(1, h.length() - 1);
        while (h.endsWith(".")) h = h.substring(0, h.length() - 1);
        if (h.isEmpty()) return true;
        if (h.contains(":")) return Ipv6.isLocal(h);
        int[] v4 = Ipv4.parse(h);
        if (v4 != null) return Ipv4.isLocal(v4);
        if (h.matches("[0-9.]+")) return true;
        if (!h.contains(".") || h.equals("localhost")) return true;
        for (String s : LOCAL_SUFFIXES) if (h.endsWith(s)) return true;
        return embeddedLocal(h);
    }

    private static boolean embeddedLocal(String h) {
        for (String s : IP_DNS) {
            if (h.equals(s.substring(1))) return true;
            if (!h.endsWith(s)) continue;
            Matcher m = EMBEDDED.matcher(h.substring(0, h.length() - s.length()));
            if (!m.find()) return true;
            int[] v4 = Ipv4.parse(m.group(1) + "." + m.group(2) + "." + m.group(3) + "." + m.group(4));
            return v4 == null || Ipv4.isLocal(v4);
        }
        return false;
    }
}

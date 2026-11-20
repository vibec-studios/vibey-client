package dev.henny.vibeyclient.module.social.presence;

public record HostPort(String host, int port) {
    public static final int DEFAULT = 25565;

    public static HostPort parse(String ip) {
        if (ip == null) return null;
        String s = ip.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("[")) {
            int close = s.indexOf(']');
            if (close < 0) return null;
            String rest = s.substring(close + 1);
            if (!rest.isEmpty() && !rest.startsWith(":")) return null;
            return of(s.substring(1, close), rest.isEmpty() ? null : rest.substring(1));
        }
        int first = s.indexOf(':');
        if (first < 0) return of(s, null);
        if (first != s.lastIndexOf(':')) return of(s, null);
        return of(s.substring(0, first), s.substring(first + 1));
    }

    private static HostPort of(String host, String port) {
        if (host.isBlank()) return null;
        if (port == null) return new HostPort(host, DEFAULT);
        if (!port.matches("\\d{1,5}")) return null;
        int p = Integer.parseInt(port);
        return p < 1 || p > 65535 ? null : new HostPort(host, p);
    }
}

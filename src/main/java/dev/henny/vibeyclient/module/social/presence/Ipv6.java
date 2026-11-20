package dev.henny.vibeyclient.module.social.presence;

final class Ipv6 {
    private Ipv6() {}

    static boolean isLocal(String h) {
        int zone = h.indexOf('%');
        if (zone >= 0) return true;
        if (!h.matches("[0-9a-f:.]+")) return true;
        if (h.equals("::") || h.equals("::1") || h.matches("(0{1,4}:){7}0{0,3}1")) return true;
        int lastColon = h.lastIndexOf(':');
        String tail = h.substring(lastColon + 1);
        if (tail.contains(".")) {
            int[] v4 = Ipv4.parse(tail);
            if (v4 == null) return true;
            String head = h.substring(0, lastColon + 1);
            if (head.equals("::ffff:") || head.equals("::")) return Ipv4.isLocal(v4);
        }
        int first = firstHextet(h);
        if (first < 0) return true;
        return (first & 0xfe00) == 0xfc00 || (first & 0xffc0) == 0xfe80 || (first & 0xff00) == 0xff00 || first == 0;
    }

    private static int firstHextet(String h) {
        if (h.startsWith("::")) return 0;
        int end = h.indexOf(':');
        String s = end < 0 ? h : h.substring(0, end);
        if (s.isEmpty() || s.length() > 4) return -1;
        return Integer.parseInt(s, 16);
    }
}

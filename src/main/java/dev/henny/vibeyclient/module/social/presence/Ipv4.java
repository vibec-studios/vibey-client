package dev.henny.vibeyclient.module.social.presence;

final class Ipv4 {
    private Ipv4() {}

    static int[] parse(String s) {
        String[] parts = s.split("\\.", -1);
        if (parts.length != 4) return null;
        int[] out = new int[4];
        for (int i = 0; i < 4; i++) {
            if (!parts[i].matches("\\d{1,3}")) return null;
            out[i] = Integer.parseInt(parts[i]);
            if (out[i] > 255) return null;
        }
        return out;
    }

    static boolean isLocal(int[] a) {
        return a[0] == 0 || a[0] == 10 || a[0] == 127 || a[0] >= 224
                || (a[0] == 169 && a[1] == 254)
                || (a[0] == 172 && a[1] >= 16 && a[1] <= 31)
                || (a[0] == 192 && a[1] == 168)
                || (a[0] == 100 && a[1] >= 64 && a[1] <= 127);
    }
}

package dev.henny.companio.parse;

final class Png {
    private Png() {}

    static int[] size(byte[] b) {
        if (b == null || b.length < 24) return null;
        if ((b[0] & 0xFF) != 0x89 || b[1] != 'P' || b[2] != 'N' || b[3] != 'G') return null;
        return new int[] {readInt(b, 16), readInt(b, 20)};
    }

    private static int readInt(byte[] b, int o) {
        return ((b[o] & 0xFF) << 24) | ((b[o + 1] & 0xFF) << 16) | ((b[o + 2] & 0xFF) << 8) | (b[o + 3] & 0xFF);
    }

    static boolean ogg(byte[] b) {
        return b != null && b.length > 4 && b[0] == 'O' && b[1] == 'g' && b[2] == 'g' && b[3] == 'S';
    }
}

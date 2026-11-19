package dev.henny.cosmetico.golden;

public final class Tables {
    private Tables() {}

    public static final String[] HAS = {"11111111", "10000000", "11100000", "11110000", "11111100", "10000011", "01111111", "11010101"};
    public static final float[][] INPUTS = {
            {0, 0, 0, 0, 0, 0, 0}, {0, 0, 1, 0, 0, 0, 0}, {0, 0, 0.6f, 0.5f, 0.2f, 0.4f, 0.3f}, {1, 1, 0.3f, 0, 0, 0, 0},
            {0.1f, 0.3f, 0.6f, 0.5f, 0.2f, 0.4f, 0.3f}, {-1, 2, 1.5f, 1, 1, 1, 1}, {0, 1, 1, 1, 0, 0, 0}};
    public static final float[][] STATE_STEPS = {
            {1.0f, 1, 0, 0, 0, 0, 0, 0, 0}, {1.1f, 1, 1, 0, 0, 0.5f, 0, 0, 0}, {1.2f, 0, 1, 0, 1, 0.2f, 0.3f, 0, 0},
            {1.3f, 0, 0, 0, 1, 0.005f, 0.2f, 0, 0.5f}, {1.4f, 0, 0, 1, 0, 0, 0.009f, 0, 0.3f}, {1.5f, 0, 0, 0, 0, 0, 0, 0.001f, 0}};
    public static final String[][] VERSIONS = {
            {"0.3.1", "0.3.0"}, {"0.3.0", "0.3.1"}, {"1.0.0-beta", "1.0.0"}, {"1.0.0", "1.0.0-beta"}, {"1.0.0-alpha", "1.0.0-beta"},
            {"v1.2", "1.2.0"}, {"1.2.0+build5", "1.2.0"}, {"1.10.0", "1.9.9"}, {"junk", "0.0.0"}, {"1.2.x", "1.2.0"}, {null, "1.0"},
            {" 2.0 ", "2"}, {"1.0.0-rc.1", "1.0.0-rc.2"}, {"0.3.1", "9.0.0"}, {"0.3.1", "0.3.1-SNAPSHOT"}, {"1.2.3.4", "1.2.3"}};

    public static boolean[] bits(String s) {
        boolean[] b = new boolean[s.length()];
        for (int i = 0; i < b.length; i++) b[i] = s.charAt(i) == '1';
        return b;
    }

    public static byte[][] thumbnails() {
        byte[] ok = png(64, 64, 100);
        byte[] wide = png(300, 10, 100);
        byte[] zero = png(0, 5, 100);
        byte[] sig = png(16, 16, 100);
        sig[1] = 'X';
        byte[] ihdr = png(16, 16, 100);
        ihdr[12] = 'X';
        return new byte[][]{null, new byte[10], ok, wide, zero, sig, ihdr, png(16, 16, 256 * 1024 + 1), png(256, 256, 256 * 1024)};
    }

    public static byte[] png(int w, int h, int size) {
        byte[] b = new byte[size];
        int[] head = {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
        for (int i = 0; i < head.length; i++) b[i] = (byte) head[i];
        put(b, 16, w);
        put(b, 20, h);
        return b;
    }

    private static void put(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }
}

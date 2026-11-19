package dev.henny.cosmetico.dev;

public final class ThumbnailCheck {
    private ThumbnailCheck() {}

    public static final int SIZE = 256, BYTES = 256 * 1024;
    private static final int[] SIGNATURE = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    public static String check(byte[] png) {
        if (png == null || png.length < 24) return "not a PNG";
        if (png.length > BYTES) return "larger than 256 KiB";
        for (int i = 0; i < SIGNATURE.length; i++) if ((png[i] & 0xFF) != SIGNATURE[i]) return "not a PNG";
        if (png[12] != 0x49 || png[13] != 0x48 || png[14] != 0x44 || png[15] != 0x52) return "not a PNG";
        long w = ((png[16] & 0xFFL) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
        long h = ((png[20] & 0xFFL) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
        if (w < 1 || h < 1 || w > SIZE || h > SIZE) return "must be at most 256x256 px (is " + w + "x" + h + ")";
        return null;
    }
}

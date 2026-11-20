package dev.henny.r3.mc.texture;

import java.io.IOException;
import java.io.InputStream;

final class PngSize {
    private static final int HEADER = 24;

    private PngSize() {}

    static int[] read(InputStream in) throws IOException {
        byte[] b = in.readNBytes(HEADER);
        if (b.length < HEADER || (b[1] & 0xFF) != 'P' || (b[2] & 0xFF) != 'N' || (b[3] & 0xFF) != 'G') return null;
        return new int[] {int32(b, 16), int32(b, 20)};
    }

    private static int int32(byte[] b, int at) {
        return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | (b[at + 3] & 0xFF);
    }
}

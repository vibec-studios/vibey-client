package dev.henny.vui.mc.texture;

import java.io.IOException;
import java.io.InputStream;

final class PngSize {
    private static final int HEADER = 24;
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private PngSize() {
    }

    static int[] read(InputStream in) throws IOException {
        byte[] b = in.readNBytes(HEADER);
        if (b.length < HEADER) return null;
        for (int i = 0; i < SIGNATURE.length; i++) if (b[i] != SIGNATURE[i]) return null;
        if (b[12] != 'I' || b[13] != 'H' || b[14] != 'D' || b[15] != 'R') return null;
        return new int[]{int32(b, 16), int32(b, 20)};
    }

    private static int int32(byte[] b, int at) {
        return ((b[at] & 0xFF) << 24) | ((b[at + 1] & 0xFF) << 16) | ((b[at + 2] & 0xFF) << 8) | (b[at + 3] & 0xFF);
    }
}

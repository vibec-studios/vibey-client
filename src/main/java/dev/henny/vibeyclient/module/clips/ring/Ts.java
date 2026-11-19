package dev.henny.vibeyclient.module.clips.ring;

/** MPEG-TS bits we need (ISO/IEC 13818-1): packet header, PAT/PMT, PES timestamps. */
final class Ts {
    static final int PACKET = 188;
    static final int SYNC = 0x47;
    static final long PTS_MASK = (1L << 33) - 1;
    static final int STREAM_H264 = 0x1B;
    static final int STREAM_AAC = 0x0F;

    private Ts() {}

    static int pid(byte[] p, int o) {
        return ((p[o + 1] & 0x1F) << 8) | (p[o + 2] & 0xFF);
    }

    static boolean unitStart(byte[] p, int o) {
        return (p[o + 1] & 0x40) != 0;
    }

    static boolean hasAdaptation(byte[] p, int o) {
        return (p[o + 3] & 0x20) != 0;
    }

    static boolean hasPayload(byte[] p, int o) {
        return (p[o + 3] & 0x10) != 0;
    }

    /** random_access_indicator of the adaptation field (set by FFmpeg on keyframes). */
    static boolean randomAccess(byte[] p, int o) {
        if (!hasAdaptation(p, o)) return false;
        int len = p[o + 4] & 0xFF;
        return len > 0 && (p[o + 5] & 0x40) != 0;
    }

    /** Offset of the payload inside the packet, or -1. */
    static int payload(byte[] p, int o) {
        if (!hasPayload(p, o)) return -1;
        int off = 4;
        if (hasAdaptation(p, o)) off += 1 + (p[o + 4] & 0xFF);
        return off < PACKET ? off : -1;
    }

    /** PTS of a PES header starting at {@code o + off}, or -1. */
    static long pesPts(byte[] p, int o, int off) {
        int a = o + off;
        int end = o + PACKET;
        if (a + 14 > end) return -1;
        if (p[a] != 0 || p[a + 1] != 0 || p[a + 2] != 1) return -1;
        int flags = (p[a + 7] & 0xFF) >> 6;
        if ((flags & 2) == 0) return -1;
        int b = a + 9;
        return (((long) (p[b] & 0x0E)) << 29) | ((p[b + 1] & 0xFFL) << 22) | (((long) (p[b + 2] & 0xFE)) << 14)
                | ((p[b + 3] & 0xFFL) << 7) | ((p[b + 4] & 0xFFL) >> 1);
    }

    /** a - b on the 33-bit PTS clock. */
    static long ptsDiff(long a, long b) {
        return (a - b) & PTS_MASK;
    }
}

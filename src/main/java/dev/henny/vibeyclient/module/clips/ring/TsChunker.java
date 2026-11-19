package dev.henny.vibeyclient.module.clips.ring;

/**
 * Cuts one continuous MPEG-TS stream (FFmpeg's stdout) into GOP chunks: a new chunk starts at every video PES whose
 * packet carries {@code random_access_indicator} (FFmpeg's mpegts muxer sets it on keyframes). The newest PAT and PMT
 * are kept so every part file written from the ring can start with them. Not thread-safe: one reader thread feeds it.
 */
public final class TsChunker {
    private final ClipRing ring;
    private final int session;
    private final byte[] carry = new byte[Ts.PACKET];
    private int carried;
    private byte[] pat;
    private byte[] pmt;
    private int pmtPid = -1;
    private int videoPid = -1;
    private int audioPid = -1;
    private Chunk open;
    private long lastPts = -1;
    private long frameTicks = 3000; // 90 kHz ticks per frame, refined from the stream
    private long skippedBytes;

    public TsChunker(ClipRing ring, int session) {
        this.ring = ring;
        this.session = session;
    }

    /** Feeds bytes as they come from the pipe (any length). */
    public void feed(byte[] buf, int off, int len) {
        if (carried > 0) {
            int n = Math.min(len, Ts.PACKET - carried);
            System.arraycopy(buf, off, carry, carried, n);
            carried += n;
            off += n;
            len -= n;
            if (carried < Ts.PACKET) return;
            packet(carry, 0);
            carried = 0;
        }
        while (len >= Ts.PACKET) {
            if ((buf[off] & 0xFF) != Ts.SYNC) {
                // lost sync (should not happen with a pipe): skip to the next sync byte
                off++;
                len--;
                skippedBytes++;
                continue;
            }
            packet(buf, off);
            off += Ts.PACKET;
            len -= Ts.PACKET;
        }
        if (len > 0) {
            System.arraycopy(buf, off, carry, 0, len);
            carried = len;
        }
    }

    /** The stream ended (FFmpeg exited): closes the open chunk with an estimated end. */
    public void end() {
        if (open != null) {
            open.close(lastPts < 0 ? open.firstPts : (lastPts + frameTicks) & Ts.PTS_MASK);
            ring.closed(open);
            open = null;
        }
    }

    public long skippedBytes() {
        return skippedBytes;
    }

    public boolean hasAudio() {
        return audioPid >= 0;
    }

    private void packet(byte[] p, int o) {
        int pid = Ts.pid(p, o);
        if (pid == 0) {
            pat = copy(p, o);
            parsePat(p, o);
        } else if (pid == pmtPid) {
            pmt = copy(p, o);
            parsePmt(p, o);
        }
        if (pid == videoPid && Ts.unitStart(p, o)) {
            int pl = Ts.payload(p, o);
            long pts = pl < 0 ? -1 : Ts.pesPts(p, o, pl);
            if (pts >= 0) {
                if (lastPts >= 0) {
                    long d = Ts.ptsDiff(pts, lastPts);
                    if (d > 0 && d < 90_000) frameTicks = (frameTicks * 7 + d) / 8;
                }
                if (Ts.randomAccess(p, o) || open == null) {
                    if (open != null) {
                        open.close(pts);
                        ring.closed(open);
                        open = null;
                    }
                    if (Ts.randomAccess(p, o) && pat != null && pmt != null) {
                        byte[] headers = new byte[Ts.PACKET * 2];
                        System.arraycopy(pat, 0, headers, 0, Ts.PACKET);
                        System.arraycopy(pmt, 0, headers, Ts.PACKET, Ts.PACKET);
                        open = ring.open(session, headers, pts);
                    }
                }
                if (open != null) open.markFrame(pts);
                lastPts = pts;
            }
        }
        if (open != null) open.append(p, o, Ts.PACKET);
    }

    private static byte[] copy(byte[] p, int o) {
        byte[] b = new byte[Ts.PACKET];
        System.arraycopy(p, o, b, 0, Ts.PACKET);
        return b;
    }

    /** Start of the PSI section inside the packet (after pointer_field), or -1. */
    private static int section(byte[] p, int o) {
        if (!Ts.unitStart(p, o)) return -1;
        int pl = Ts.payload(p, o);
        if (pl < 0) return -1;
        int s = pl + 1 + (p[o + pl] & 0xFF);
        return s + 8 < Ts.PACKET ? s : -1;
    }

    private void parsePat(byte[] p, int o) {
        int s = section(p, o);
        if (s < 0 || (p[o + s] & 0xFF) != 0) return;
        int len = ((p[o + s + 1] & 0x0F) << 8) | (p[o + s + 2] & 0xFF);
        int end = Math.min(Ts.PACKET, s + 3 + len - 4);
        for (int i = s + 8; i + 4 <= end; i += 4) {
            int program = ((p[o + i] & 0xFF) << 8) | (p[o + i + 1] & 0xFF);
            int pid = ((p[o + i + 2] & 0x1F) << 8) | (p[o + i + 3] & 0xFF);
            if (program != 0) {
                pmtPid = pid;
                return;
            }
        }
    }

    private void parsePmt(byte[] p, int o) {
        int s = section(p, o);
        if (s < 0 || (p[o + s] & 0xFF) != 2) return;
        int len = ((p[o + s + 1] & 0x0F) << 8) | (p[o + s + 2] & 0xFF);
        int end = Math.min(Ts.PACKET, s + 3 + len - 4);
        int infoLen = ((p[o + s + 10] & 0x0F) << 8) | (p[o + s + 11] & 0xFF);
        for (int i = s + 12 + infoLen; i + 5 <= end; ) {
            int type = p[o + i] & 0xFF;
            int pid = ((p[o + i + 1] & 0x1F) << 8) | (p[o + i + 2] & 0xFF);
            int esLen = ((p[o + i + 3] & 0x0F) << 8) | (p[o + i + 4] & 0xFF);
            if (type == Ts.STREAM_H264 && videoPid < 0) videoPid = pid;
            if (type == Ts.STREAM_AAC && audioPid < 0) audioPid = pid;
            i += 5 + esLen;
        }
    }
}

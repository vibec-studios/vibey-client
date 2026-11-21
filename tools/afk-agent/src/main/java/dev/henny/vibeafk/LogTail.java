package dev.henny.vibeafk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class LogTail {
    private static final int MAX_CHUNK = 512 * 1024;
    private static final int HEAD = 160;

    private final File file;
    private long offset;
    private byte[] carry = new byte[0];
    private String head;

    public LogTail(File file) {
        this.file = file;
    }

    public interface Sink {
        void line(String line);
    }

    public boolean exists() {
        return file.isFile();
    }

    public void skipExisting() {
        if (!file.isFile()) return;
        offset = file.length();
        head = readHead();
    }

    public void poll(Sink sink) {
        if (!file.isFile()) return;
        long len = file.length();
        String h = readHead();
        boolean replaced = head != null && h != null && !sameStart(head, h);
        if (len < offset || replaced) {
            offset = 0;
            carry = new byte[0];
        }
        if (h != null && (head == null || replaced || h.length() > head.length())) head = h;
        if (len == offset) return;
        int n = (int) Math.min(MAX_CHUNK, len - offset);
        byte[] buf = new byte[carry.length + n];
        System.arraycopy(carry, 0, buf, 0, carry.length);
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            raf.seek(offset);
            raf.readFully(buf, carry.length, n);
        } catch (IOException e) {
            return;
        } finally {
            OutboxReader.closeQuietly(raf);
        }
        offset += n;
        int start = 0;
        for (int k = 0; k < buf.length; k++) {
            if (buf[k] == '\n') {
                int end = k > start && buf[k - 1] == '\r' ? k - 1 : k;
                sink.line(new String(buf, start, end - start, Fs.UTF8));
                start = k + 1;
            }
        }
        int rest = buf.length - start;
        if (rest > 64 * 1024) rest = 0;
        carry = new byte[rest];
        System.arraycopy(buf, buf.length - rest, carry, 0, rest);
    }

    private static boolean sameStart(String a, String b) {
        int n = Math.min(a.length(), b.length());
        return a.regionMatches(0, b, 0, n);
    }

    private String readHead() {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            int n = (int) Math.min(HEAD, raf.length());
            if (n <= 0) return null;
            byte[] b = new byte[n];
            raf.readFully(b);
            return new String(b, "ISO-8859-1");
        } catch (IOException e) {
            return null;
        } finally {
            OutboxReader.closeQuietly(raf);
        }
    }
}

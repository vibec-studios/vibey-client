package dev.henny.vibeafk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public final class OutboxReader {
    public static final String FILE = "afk-outbox.jsonl";
    public static final long MAX_BYTES = 256L * 1024;

    private final File file;
    private long offset;
    private boolean tooLargeReported;

    public OutboxReader(File dir) {
        this.file = new File(dir, FILE);
    }

    public interface Sink {
        void line(String line);

        void tooLarge();
    }

    public void poll(Sink sink) {
        if (!file.isFile()) {
            offset = 0;
            return;
        }
        long len = file.length();
        if (len < offset) offset = 0;
        if (len == offset) return;
        if (len > MAX_BYTES) {
            if (!tooLargeReported) sink.tooLarge();
            tooLargeReported = true;
            return;
        }
        tooLargeReported = false;
        byte[] buf;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            raf.seek(offset);
            buf = new byte[(int) (len - offset)];
            raf.readFully(buf);
        } catch (IOException e) {
            return;
        } finally {
            closeQuietly(raf);
        }
        int start = 0;
        List<String> lines = new ArrayList<String>();
        for (int k = 0; k < buf.length; k++) {
            if (buf[k] == '\n') {
                lines.add(new String(buf, start, k - start, Fs.UTF8).trim());
                start = k + 1;
            }
        }
        offset += start;
        for (String l : lines) if (!l.isEmpty()) sink.line(l);
    }

    static void closeQuietly(RandomAccessFile raf) {
        if (raf == null) return;
        try {
            raf.close();
        } catch (IOException ignored) {
        }
    }
}

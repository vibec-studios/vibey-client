package dev.henny.vibeyclient.module.clips.ring;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class DiskBody implements ChunkBody {
    static final int BUFFER = 64 * 1024;
    private final DiskStorage owner;
    private final Path file;
    private FileChannel channel;
    private byte[] pending = new byte[BUFFER];
    private int buffered;
    private long size;
    private boolean broken;

    DiskBody(DiskStorage owner, Path file) {
        this.owner = owner;
        this.file = file;
    }

    @Override
    public void append(byte[] src, int off, int len) {
        if (broken || pending == null) return;
        while (len > 0) {
            int n = Math.min(len, BUFFER - buffered);
            System.arraycopy(src, off, pending, buffered, n);
            buffered += n;
            off += n;
            len -= n;
            size += n;
            if (buffered == BUFFER) flush();
        }
    }

    private void flush() {
        if (buffered == 0 || broken || pending == null) return;
        try {
            if (channel == null) {
                Files.createDirectories(file.getParent());
                channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE, StandardOpenOption.READ);
            }
            ByteBuffer b = ByteBuffer.wrap(pending, 0, buffered);
            while (b.hasRemaining()) channel.write(b);
            buffered = 0;
        } catch (IOException e) {
            broken = true;
            owner.failed(e);
        }
    }

    @Override
    public long size() {
        return broken ? 0 : size;
    }

    @Override
    public void seal() {
        flush();
        closeChannel();
        pending = null;
    }

    @Override
    public boolean writeTo(OutputStream out, long end) throws IOException {
        if (broken) return false;
        flush();
        if (broken) return false;
        if (end <= 0) return true;
        if (channel != null) {
            copy(channel, out, end);
            return true;
        }
        try (FileChannel in = FileChannel.open(file, StandardOpenOption.READ)) {
            copy(in, out, end);
        }
        return true;
    }

    private static void copy(FileChannel in, OutputStream out, long end) throws IOException {
        byte[] a = new byte[BUFFER];
        ByteBuffer b = ByteBuffer.wrap(a);
        long pos = 0;
        while (pos < end) {
            b.clear();
            b.limit((int) Math.min(BUFFER, end - pos));
            int n = in.read(b, pos);
            if (n < 0) throw new IOException("buffer file ended early");
            out.write(a, 0, n);
            pos += n;
        }
    }

    @Override
    public void release() {
        closeChannel();
        pending = null;
        buffered = 0;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private void closeChannel() {
        if (channel == null) return;
        try {
            channel.close();
        } catch (IOException ignored) {
        }
        channel = null;
    }
}

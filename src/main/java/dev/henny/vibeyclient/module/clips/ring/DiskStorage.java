package dev.henny.vibeyclient.module.clips.ring;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DiskStorage implements ChunkStorage {
    private final Path dir;
    private long seq;
    private volatile IOException error;

    DiskStorage(Path dir) {
        this.dir = dir;
    }

    public Path dir() {
        return dir;
    }

    @Override
    public synchronized ChunkBody create(int session) {
        return new DiskBody(this, dir.resolve("s" + session + "-" + (++seq) + ".ts"));
    }

    void failed(IOException e) {
        if (error == null) error = e;
    }

    @Override
    public IOException error() {
        return error;
    }

    @Override
    public boolean onDisk() {
        return true;
    }

    @Override
    public void clear() {
        wipe(dir);
    }

    public static void wipe(Path dir) {
        if (!Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> s = Files.newDirectoryStream(dir)) {
            for (Path p : s) {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        try {
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
        }
    }
}

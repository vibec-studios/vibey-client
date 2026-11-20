package dev.henny.companio.pack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class DirFiles implements PackFiles {
    private final Path root;

    public DirFiles(Path root) {
        this.root = root;
    }

    public String name() {
        return root.getFileName().toString();
    }

    public String location() {
        return root.toString();
    }

    public List<String> list() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.walk(root, 8)) {
            s.filter(Files::isRegularFile).limit(PackLimits.MAX_FILES + 1L)
                    .forEach(p -> out.add(root.relativize(p).toString().replace('\\', '/')));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    public boolean exists(String path) {
        return PackPaths.valid(path) && Files.isRegularFile(root.resolve(path));
    }

    public byte[] read(String path) throws IOException {
        if (!PackPaths.valid(path)) throw new IOException("bad path " + path);
        Path p = root.resolve(path);
        if (Files.size(p) > PackLimits.MAX_FILE_BYTES) throw new IOException("file is larger than " + PackLimits.MAX_FILE_BYTES / 1024 / 1024 + " MiB");
        return Files.readAllBytes(p);
    }

    public long stamp() {
        long[] max = {0};
        long[] count = {0};
        try (Stream<Path> s = Files.walk(root, 8)) {
            s.limit(PackLimits.MAX_FILES * 2L).forEach(p -> {
                count[0]++;
                try {
                    max[0] = Math.max(max[0], Files.getLastModifiedTime(p).toMillis());
                } catch (IOException ignored) {
                    max[0]++;
                }
            });
        } catch (IOException | UncheckedIOException e) {
            return -1;
        }
        return max[0] * 31 + count[0];
    }
}

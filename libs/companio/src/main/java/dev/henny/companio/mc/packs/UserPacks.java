package dev.henny.companio.mc.packs;

import dev.henny.companio.pack.DirFiles;
import dev.henny.companio.pack.MapFiles;
import dev.henny.companio.pack.PackFiles;
import dev.henny.companio.pack.PackLimits;
import dev.henny.companio.pack.ZipFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class UserPacks {
    private final Path dir;

    public UserPacks(Path dir) {
        this.dir = dir;
    }

    public Path dir() {
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }

    public List<Path> entries() {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> Files.isDirectory(p) ? Files.isRegularFile(p.resolve(PackLimits.MANIFEST))
                            : p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .sorted().forEach(out::add);
        } catch (IOException ignored) {
        }
        return out;
    }

    public long stamp() {
        long h = 17;
        for (Path p : entries()) {
            h = h * 31 + p.getFileName().toString().hashCode();
            h = h * 31 + (Files.isDirectory(p) ? new DirFiles(p).stamp() : mtime(p));
        }
        return h;
    }

    private static long mtime(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis() * 31 + Files.size(p);
        } catch (IOException e) {
            return -1;
        }
    }

    public static PackFiles open(Path p) {
        try {
            return Files.isDirectory(p) ? new DirFiles(p) : ZipFiles.read(p);
        } catch (IOException e) {
            return new MapFiles(p.getFileName().toString(), p.toString(), Map.of(), 0);
        }
    }

}

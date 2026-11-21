package dev.henny.vibeafk;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Fs {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    private Fs() {}

    public static String read(File f, long max) {
        try {
            if (!f.isFile() || f.length() > max) return null;
            return new String(Files.readAllBytes(f.toPath()), UTF8);
        } catch (IOException e) {
            return null;
        }
    }

    public static void writeAtomic(File target, String text) throws IOException {
        Path dir = target.getAbsoluteFile().getParentFile().toPath();
        Files.createDirectories(dir);
        Path tmp = dir.resolve(target.getName() + ".tmp");
        Files.write(tmp, text.getBytes(UTF8));
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                try {
                    Files.move(tmp, target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (IOException e) {
                last = e;
                sleep(40);
            }
        }
        throw last;
    }

    public static void deleteQuietly(File f) {
        try {
            Files.deleteIfExists(f.toPath());
        } catch (IOException ignored) {
        }
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

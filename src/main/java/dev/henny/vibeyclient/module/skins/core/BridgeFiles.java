package dev.henny.vibeyclient.module.skins.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class BridgeFiles {
    public static final String LIBRARY = "skins.json";
    public static final String SKIN_DIR = "skins";
    public static final String REQUEST = "skin-request.json";
    public static final String RESULT = "skin-result.json";
    public static final long MAX_PNG = 512 * 1024;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private BridgeFiles() {}

    public static void writeRequest(Path vibeDir, SkinRequest r) throws IOException {
        writeAtomic(vibeDir, REQUEST, r.toJson());
    }

    public static SkinLibrary readLibrary(Path vibeDir) {
        String s = read(vibeDir.resolve(LIBRARY), 4L * 1024 * 1024);
        return s == null ? null : SkinLibrary.parse(s);
    }

    public static SkinResult readResult(Path vibeDir) {
        String s = read(vibeDir.resolve(RESULT), 64L * 1024);
        return s == null ? null : SkinResult.parse(s);
    }

    public static Path png(Path vibeDir, LibrarySkin s) {
        return vibeDir.resolve(SKIN_DIR).resolve(s.file());
    }

    public static byte[] readPng(Path vibeDir, LibrarySkin s) {
        Path p = png(vibeDir, s);
        try {
            if (!Files.isRegularFile(p) || Files.size(p) > MAX_PNG) return null;
            return Files.readAllBytes(p);
        } catch (IOException e) {
            return null;
        }
    }

    public static long mtime(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }

    public static void clearOwn(Path vibeDir) {
        for (String f : new String[]{REQUEST, REQUEST + ".tmp", RESULT}) {
            try {
                Files.deleteIfExists(vibeDir.resolve(f));
            } catch (IOException ignored) {
            }
        }
    }

    private static String read(Path p, long max) {
        try {
            if (!Files.isRegularFile(p) || Files.size(p) > max) return null;
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    static void writeAtomic(Path dir, String name, JsonObject json) throws IOException {
        Files.createDirectories(dir);
        Path target = dir.resolve(name);
        Path tmp = dir.resolve(name + ".tmp");
        Files.writeString(tmp, GSON.toJson(json), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

package dev.henny.companio.pack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ZipFiles {
    private ZipFiles() {}

    public static MapFiles read(Path zip) throws IOException {
        long stamp = Files.getLastModifiedTime(zip).toMillis() * 31 + Files.size(zip);
        try (InputStream in = Files.newInputStream(zip)) {
            String name = zip.getFileName().toString().replaceFirst("(?i)\\.zip$", "");
            return new MapFiles(name, zip.toString(), entries(in), stamp);
        }
    }

    static Map<String, byte[]> entries(InputStream raw) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        long total = 0;
        try (ZipInputStream in = new ZipInputStream(raw)) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String path = e.getName().replace('\\', '/');
                if (!PackPaths.valid(path)) continue;
                byte[] b = readLimited(in);
                total += b.length;
                if (total > PackLimits.MAX_PACK_BYTES) throw new IOException("pack is larger than " + PackLimits.MAX_PACK_BYTES / 1024 / 1024 + " MiB");
                if (files.size() >= PackLimits.MAX_FILES) throw new IOException("pack has more than " + PackLimits.MAX_FILES + " files");
                files.put(path, b);
            }
        }
        return stripTopFolder(files);
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        byte[] b = in.readNBytes(PackLimits.MAX_FILE_BYTES + 1);
        if (b.length > PackLimits.MAX_FILE_BYTES) throw new IOException("a file is larger than " + PackLimits.MAX_FILE_BYTES / 1024 / 1024 + " MiB");
        return b;
    }

    static Map<String, byte[]> stripTopFolder(Map<String, byte[]> files) {
        if (files.containsKey(PackLimits.MANIFEST)) return files;
        String top = null;
        for (String p : files.keySet()) {
            if (p.endsWith("/" + PackLimits.MANIFEST) && p.indexOf('/') == p.length() - PackLimits.MANIFEST.length() - 1) top = p.substring(0, p.indexOf('/') + 1);
        }
        if (top == null) return files;
        Map<String, byte[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            if (e.getKey().startsWith(top)) out.put(e.getKey().substring(top.length()), e.getValue());
        }
        return out;
    }
}

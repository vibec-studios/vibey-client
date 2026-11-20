package dev.henny.companio.pack;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MapFiles implements PackFiles {
    private final String name, location;
    private final Map<String, byte[]> files;
    private final long stamp;

    public MapFiles(String name, String location, Map<String, byte[]> files, long stamp) {
        this.name = name;
        this.location = location;
        this.files = new LinkedHashMap<>(files);
        this.stamp = stamp;
    }

    public static MapFiles ofText(String name, Map<String, String> texts) {
        Map<String, byte[]> m = new LinkedHashMap<>();
        texts.forEach((k, v) -> m.put(k, v.getBytes(StandardCharsets.UTF_8)));
        return new MapFiles(name, "memory:" + name, m, 0);
    }

    public MapFiles with(String path, byte[] bytes) {
        Map<String, byte[]> m = new LinkedHashMap<>(files);
        m.put(path, bytes);
        return new MapFiles(name, location, m, stamp + 1);
    }

    public String name() {
        return name;
    }

    public String location() {
        return location;
    }

    public List<String> list() {
        return new ArrayList<>(files.keySet());
    }

    public boolean exists(String path) {
        return files.containsKey(path);
    }

    public byte[] read(String path) throws FileNotFoundException {
        byte[] b = files.get(path);
        if (b == null) throw new FileNotFoundException(path);
        return b;
    }

    public long stamp() {
        return stamp;
    }
}

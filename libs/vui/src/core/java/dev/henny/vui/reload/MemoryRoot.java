package dev.henny.vui.reload;

import java.util.HashMap;
import java.util.Map;

public final class MemoryRoot implements ResourceRoot {
    private final Map<String, String> files = new HashMap<>();

    public MemoryRoot put(String id, String source) {
        DocumentId d = DocumentId.parse(id);
        files.put(d.resource(source.trim().startsWith("{") ? DocumentId.JSON : DocumentId.MARKUP), source);
        return this;
    }

    public MemoryRoot putFile(String relativePath, String source) {
        files.put(relativePath, source);
        return this;
    }

    @Override
    public String read(String relativePath) {
        return files.get(relativePath);
    }

    @Override
    public String describe(String relativePath) {
        return "memory:" + relativePath;
    }
}

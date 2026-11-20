package dev.henny.vui.dev;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.expr.MapTranslator;
import dev.henny.vui.json.JsonValue;
import dev.henny.vui.json.PosJsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class LangFiles {
    private LangFiles() {
    }

    public static void load(Path path, MapTranslator into, Diagnostics diagnostics) throws IOException {
        if (Files.isDirectory(path)) {
            List<Path> files;
            try (Stream<Path> list = Files.list(path)) {
                files = list.filter(p -> p.toString().endsWith(".json")).sorted().toList();
            }
            for (Path f : files) file(f, into, diagnostics);
        } else if (Files.isRegularFile(path)) {
            file(path, into, diagnostics);
        } else {
            diagnostics.error(new dev.henny.vui.diag.SourcePos(path.toString(), 1, 1), "language file or folder not found", null);
        }
    }

    public static void file(Path file, MapTranslator into, Diagnostics diagnostics) throws IOException {
        String name = file.getFileName().toString();
        String prefix = name.substring(0, name.length() - ".json".length());
        JsonValue root = PosJsonParser.parse(Files.readString(file, StandardCharsets.UTF_8), file.toString(), diagnostics);
        if (root != null && root.isObject()) flatten(prefix, root, into);
    }

    static void flatten(String prefix, JsonValue value, MapTranslator into) {
        for (Map.Entry<String, JsonValue> e : value.asObject().entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            JsonValue v = e.getValue();
            if (v.isObject()) flatten(key, v, into);
            else if (!v.isArray() && !v.isNull()) into.put(key, v.asString());
        }
    }
}

package dev.henny.companio.mc.packs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.companio.mc.CompanioMod;
import dev.henny.companio.pack.MapFiles;
import dev.henny.companio.pack.PackFiles;
import dev.henny.companio.pack.PackPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class BuiltinPacks {
    private BuiltinPacks() {}

    public static List<PackFiles> read(List<Path> assetRoots) {
        List<PackFiles> out = new ArrayList<>();
        for (Path assets : assetRoots) {
            if (!Files.isDirectory(assets)) continue;
            try (Stream<Path> namespaces = Files.list(assets)) {
                for (Path ns : namespaces.sorted().toList()) {
                    Path dir = ns.resolve("companionpacks");
                    if (Files.isRegularFile(dir.resolve("packs.json"))) out.addAll(index(dir));
                }
            } catch (IOException e) {
                CompanioMod.LOG.warn("Cannot list {}", assets, e);
            }
        }
        return out;
    }

    private static List<PackFiles> index(Path dir) throws IOException {
        List<PackFiles> out = new ArrayList<>();
        JsonObject index = JsonParser.parseString(Files.readString(dir.resolve("packs.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        for (JsonElement e : index.getAsJsonArray("packs")) {
            JsonObject p = e.getAsJsonObject();
            String id = p.get("id").getAsString();
            Map<String, byte[]> files = new LinkedHashMap<>();
            for (JsonElement f : p.getAsJsonArray("files")) {
                String path = f.getAsString();
                if (PackPaths.valid(path) && PackPaths.valid(id)) files.put(path, Files.readAllBytes(dir.resolve(id).resolve(path)));
            }
            out.add(new MapFiles(id, "builtin:" + dir.getParent().getFileName() + "/" + id, files, 0));
        }
        return out;
    }
}

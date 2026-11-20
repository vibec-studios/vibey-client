package dev.henny.companio.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.pack.Pack;
import dev.henny.companio.pack.PackFiles;
import dev.henny.companio.pack.PackHash;
import dev.henny.companio.pack.PackLimits;
import dev.henny.companio.pack.Problems;
import dev.henny.companio.text.Lang;
import dev.henny.companio.text.TextRef;
import dev.henny.cosmetico.version.VersionGate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PackReader {
    private PackReader() {}

    public static Pack read(PackFiles files, VersionGate gate, boolean builtin) {
        Problems p = new Problems();
        long stamp = files.stamp();
        List<String> list;
        try {
            list = files.list();
        } catch (RuntimeException e) {
            p.error("", "cannot read the pack: " + e.getMessage());
            return failed(files, p, builtin, stamp);
        }
        if (list.size() > PackLimits.MAX_FILES) p.error("", "more than " + PackLimits.MAX_FILES + " files");
        p.file(PackLimits.MANIFEST);
        JsonObject m = JsonFiles.object(files, PackLimits.MANIFEST, p);
        if (m == null || !header(m, p)) return failed(files, p, builtin, stamp);
        String id = Json.str(m, "id", "", p, null);
        if (id == null || !id.matches(PackLimits.PACK_ID)) {
            p.error("id", id == null ? "needs an id" : "\"" + id + "\" must be lowercase a-z, 0-9, _ . - (at most 48)");
            return failed(files, p, builtin, stamp);
        }
        Lang lang = LangReader.read(files, p);
        p.file(PackLimits.MANIFEST);
        TextRef name = TextReader.read(m, "name", "", lang, p, TextRef.literal(id));
        TextRef description = TextReader.read(m, "description", "", lang, p, TextRef.EMPTY);
        String author = Json.str(m, "author", "", p, "");
        String version = Json.str(m, "version", "", p, "");
        Map<String, byte[]> sounds = SoundFiles.read(files, list, p);
        List<CompanionDef> companions = companions(files, m, id, lang, sounds, gate, p);
        byte[] icon = icon(files);
        return new Pack(id, version, name, description, author, builtin, files.location(), PackHash.of(files), icon, lang,
                List.copyOf(companions), p.all(), stamp);
    }

    private static boolean header(JsonObject m, Problems p) {
        Json.known(m, "", p, "format", "formatVersion", "id", "version", "name", "description", "author", "companions");
        String format = Json.str(m, "format", "", p, null);
        if (!PackLimits.FORMAT.equals(format)) {
            p.error("format", "must be \"" + PackLimits.FORMAT + "\"");
            return false;
        }
        int v = (int) Json.num(m, "formatVersion", "", p, 0, 0, 1000);
        if (v > PackLimits.FORMAT_VERSION) {
            p.error("formatVersion", "needs a newer companio (pack format " + v + ", this one reads " + PackLimits.FORMAT_VERSION + ")");
            return false;
        }
        if (v < 1) {
            p.error("formatVersion", "must be 1");
            return false;
        }
        return true;
    }

    private static List<CompanionDef> companions(PackFiles files, JsonObject m, String packId, Lang lang, Map<String, byte[]> sounds,
                                                 VersionGate gate, Problems p) {
        List<CompanionDef> out = new ArrayList<>();
        p.file(PackLimits.MANIFEST);
        JsonArray a = Json.arr(m, "companions", "", p);
        if (a == null || a.isEmpty()) {
            p.error("companions", "needs at least one companion");
            return out;
        }
        if (a.size() > PackLimits.MAX_COMPANIONS) p.error("companions", "at most " + PackLimits.MAX_COMPANIONS + " companions");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < Math.min(a.size(), PackLimits.MAX_COMPANIONS); i++) {
            p.file(PackLimits.MANIFEST);
            JsonElement e = a.get(i);
            String file;
            JsonObject c;
            if (Json.isString(e)) {
                file = e.getAsString();
                p.file(file);
                c = JsonFiles.object(files, file, p);
            } else if (e.isJsonObject()) {
                file = PackLimits.MANIFEST + " companions[" + i + "]";
                c = e.getAsJsonObject();
            } else {
                p.error(Json.at("companions", i), "must be a path to a companion file or a companion object");
                continue;
            }
            if (c == null) continue;
            CompanionDef d = CompanionReader.read(files, c, file, packId, lang, sounds, gate, p);
            if (d == null) continue;
            if (!ids.add(d.id())) p.error("id", "companion \"" + d.id() + "\" exists twice in the pack");
            else out.add(d);
        }
        return out;
    }

    private static byte[] icon(PackFiles files) {
        try {
            if (!files.exists("pack.png")) return null;
            byte[] b = files.read("pack.png");
            return Png.size(b) == null ? null : b;
        } catch (IOException e) {
            return null;
        }
    }

    private static Pack failed(PackFiles files, Problems p, boolean builtin, long stamp) {
        return new Pack(null, "", TextRef.literal(files.name()), TextRef.EMPTY, "", builtin, files.location(), "", icon(files),
                Lang.EMPTY, List.of(), p.all(), stamp);
    }
}

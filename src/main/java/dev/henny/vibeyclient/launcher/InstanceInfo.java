package dev.henny.vibeyclient.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@code <instance>/.vibe/instance.json}, written by the Vibe Launcher at every start; the client only reads it
 * (contract: vibe-launcher {@code docs/PLAY-LINKS.md} §2). Parsed tolerantly: unknown fields and a newer schema are
 * fine, broken entries are skipped, a missing or unreadable file gives {@link #absent()}. Pure Java (no game classes).
 */
public final class InstanceInfo {
    public static final String DIR = ".vibe";
    public static final String FILE = "instance.json";
    /** The backend accepts at most this many mod entries per play link. */
    public static final int MAX_MODS = 500;

    private static final Set<String> SOURCES = Set.of("modrinth", "curseforge");
    private static final Set<String> KINDS = Set.of("mod", "resourcepack", "shader", "datapack");
    private static final Set<String> LOADERS = Set.of("vanilla", "fabric", "quilt", "forge", "neoforge");

    /** One content entry with a known source. {@code versionId}, {@code kind} and {@code file} may be null. */
    public record Mod(String source, String projectId, String versionId, String kind, String file) {
        /** Counts as a mod in {@code mods/} (entries without {@code kind} are mods). */
        public boolean isMod() {
            return kind == null || kind.equals("mod");
        }
    }

    /** The modpack, or null in {@link #pack()} when the instance has none. */
    public record Pack(String source, String projectId, String versionId, String title) {}

    /**
     * A shared Vibe pack ({@code vibe_pack}: share code, installed version; contract 02), or null in {@link #vibePack()}.
     * {@code versionId} is null for an instance the player shared themselves (the backend then takes the newest version).
     */
    public record VibePack(String code, String versionId) {}

    private final boolean present;
    private final int schema;
    private final String name;
    private final String mcVersion;
    private final String loaderKind;
    private final String loaderVersion;
    private final Pack pack;
    private final VibePack vibePack;
    private final List<Mod> mods;
    private final String clientPack;
    private final Set<String> knownFiles;

    private InstanceInfo(boolean present, int schema, String name, String mcVersion, String loaderKind, String loaderVersion,
                         Pack pack, VibePack vibePack, List<Mod> mods, String clientPack, Set<String> knownFiles) {
        this.present = present;
        this.schema = schema;
        this.name = name;
        this.mcVersion = mcVersion;
        this.loaderKind = loaderKind;
        this.loaderVersion = loaderVersion;
        this.pack = pack;
        this.vibePack = vibePack;
        this.mods = mods;
        this.clientPack = clientPack;
        this.knownFiles = knownFiles;
    }

    /** No {@code instance.json} (another launcher, dev run) or not readable. */
    public static InstanceInfo absent() {
        return new InstanceInfo(false, 0, null, null, null, null, null, null, List.of(), null, Set.of());
    }

    // ============================================================================================ reading

    /** Reads {@code <instanceDir>/.vibe/instance.json}; {@link #absent()} when missing or broken. Blocking I/O. */
    public static InstanceInfo read(Path instanceDir) {
        Path file = instanceDir.resolve(DIR).resolve(FILE);
        if (!Files.isRegularFile(file)) return absent();
        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            return absent();
        }
    }

    /** Parses the file's text; never throws ({@link #absent()} for anything that is not a JSON object). */
    public static InstanceInfo parse(String json) {
        JsonObject o;
        try {
            JsonElement e = JsonParser.parseString(json == null ? "" : json);
            if (!e.isJsonObject()) return absent();
            o = e.getAsJsonObject();
        } catch (RuntimeException e) {
            return absent();
        }
        int schema = num(o.get("schema"), 1);
        String name = clip(str(o.get("name")), 120);
        String mc = str(o.get("mc_version"));
        String loaderKind = null;
        String loaderVersion = null;
        JsonElement le = o.get("loader");
        if (le != null && le.isJsonObject()) {
            loaderKind = lower(str(le.getAsJsonObject().get("kind")));
            loaderVersion = str(le.getAsJsonObject().get("version"));
        } else if (le != null && le.isJsonPrimitive()) {
            loaderKind = lower(str(le)); // tolerate "loader": "fabric"
        }
        if (loaderKind != null && !LOADERS.contains(loaderKind)) loaderKind = null;

        Pack pack = null;
        JsonElement pe = o.get("pack");
        if (pe != null && pe.isJsonObject()) {
            JsonObject p = pe.getAsJsonObject();
            String src = lower(str(p.get("source")));
            String pid = id(str(p.get("project_id")));
            String vid = id(str(p.get("version_id")));
            if (src != null && SOURCES.contains(src) && pid != null && vid != null) {
                pack = new Pack(src, pid, vid, clip(str(p.get("title")), 120));
            }
        }

        VibePack vibePack = null;
        JsonElement ve = o.get("vibe_pack");
        if (ve != null && ve.isJsonObject()) {
            JsonObject v = ve.getAsJsonObject();
            String code = str(v.get("code"));
            // as the backend accepts it: 4-16 letters/digits, version id 1-64 characters (else sent without one)
            if (code != null && code.matches("[A-Za-z0-9]{4,16}")) vibePack = new VibePack(code, id(str(v.get("version_id"))));
        }

        List<Mod> mods = new ArrayList<>();
        Set<String> known = new HashSet<>();
        JsonElement me = o.get("mods");
        if (me != null && me.isJsonArray()) {
            for (JsonElement el : me.getAsJsonArray()) {
                if (!el.isJsonObject()) continue;
                JsonObject m = el.getAsJsonObject();
                String file = fileName(str(m.get("file")));
                String kind = lower(str(m.get("kind")));
                if (kind != null && !KINDS.contains(kind)) kind = kind.equals("shaderpack") ? "shader" : null;
                if (file != null && (kind == null || kind.equals("mod"))) known.add(file.toLowerCase(Locale.ROOT));
                String src = lower(str(m.get("source")));
                String pid = id(str(m.get("project_id")));
                if (src == null || !SOURCES.contains(src) || pid == null) continue;
                mods.add(new Mod(src, pid, id(str(m.get("version_id"))), kind, file));
            }
        }
        JsonElement kf = o.get("known_files");
        if (kf != null && kf.isJsonArray()) {
            for (JsonElement el : kf.getAsJsonArray()) {
                String path = str(el);
                if (path == null) continue;
                path = path.replace('\\', '/');
                // only mods/ matter for the hint; bare names count as mods/<name>
                if (path.contains("/") && !path.toLowerCase(Locale.ROOT).startsWith("mods/")) continue;
                String f = fileName(path);
                if (f != null) known.add(f.toLowerCase(Locale.ROOT));
            }
        }
        String clientPack = lower(str(o.get("client_pack")));
        if (clientPack != null && !clientPack.matches("[a-z0-9_-]{1,32}")) clientPack = null;
        return new InstanceInfo(true, schema, name, mc, loaderKind, loaderVersion, pack, vibePack,
                Collections.unmodifiableList(mods), clientPack, Collections.unmodifiableSet(known));
    }

    // ============================================================================================ accessors

    public boolean present() {
        return present;
    }

    public int schema() {
        return schema;
    }

    /** Instance name from the launcher, or null. */
    public String name() {
        return name;
    }

    public String mcVersion() {
        return mcVersion;
    }

    /** {@code fabric}, {@code neoforge} … or null. */
    public String loaderKind() {
        return loaderKind;
    }

    public String loaderVersion() {
        return loaderVersion;
    }

    public Pack pack() {
        return pack;
    }

    /** The shared Vibe pack the instance comes from (or was shared as), or null. */
    public VibePack vibePack() {
        return vibePack;
    }

    public List<Mod> mods() {
        return mods;
    }

    public String clientPack() {
        return clientPack;
    }

    /** Lower-case file names in {@code mods/} whose source the launcher knows (from {@code file} / {@code known_files}). */
    public Set<String> knownFiles() {
        return knownFiles;
    }

    // ============================================================================================ manual mods

    /**
     * How many jars in {@code mods/} the launcher does not know (added by hand), or -1 when that cannot be told
     * (no {@code instance.json}, or a modpack without file names). Uses the file names when the launcher sends
     * them ({@code mods[].file} / {@code known_files}), otherwise compares counts (instances without a modpack).
     * Our own jar ({@code vibey-client-*} / {@code vibeyclient*}) never counts: the launcher installs it as the client pack.
     */
    public int manualMods(Path modsDir) {
        if (!present) return -1;
        List<String> jars = jars(modsDir);
        if (!knownFiles.isEmpty()) {
            int n = 0;
            for (String j : jars) if (!knownFiles.contains(j)) n++;
            return n;
        }
        if (pack != null) return -1;
        int listed = 0;
        for (Mod m : mods) if (m.isMod()) listed++;
        return Math.max(0, jars.size() - listed);
    }

    /** Lower-case names of the active jars in {@code modsDir}, without our own. */
    static List<String> jars(Path modsDir) {
        List<String> out = new ArrayList<>();
        if (modsDir == null || !Files.isDirectory(modsDir)) return out;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(modsDir, "*.jar")) {
            for (Path p : ds) {
                if (!Files.isRegularFile(p)) continue;
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (n.startsWith("vibeyclient") || n.startsWith("vibey-client")) continue; // our own jar (both names)
                out.add(n);
            }
        } catch (IOException ignored) {
        }
        return out;
    }

    // ============================================================================================ play link body

    /**
     * The instance part of {@code POST /api/v1/play-links} (§3): {@code pack}, {@code mods}, {@code client_pack} as
     * the backend accepts them (only modrinth/curseforge with ids, at most {@link #MAX_MODS} entries). Empty when absent.
     * With a {@link #vibePack()} it is sent as {@code vibe_pack} unchanged and {@code pack} is left out (both = 400).
     */
    public JsonObject playLinkFields() {
        return playLinkFields(true);
    }

    /** As {@link #playLinkFields()}; {@code withVibePack = false} for a backend that does not know {@code vibe_pack}. */
    public JsonObject playLinkFields(boolean withVibePack) {
        JsonObject o = new JsonObject();
        if (!present) return o;
        if (withVibePack && vibePack != null) {
            JsonObject v = new JsonObject();
            v.addProperty("code", vibePack.code());
            if (vibePack.versionId() != null) v.addProperty("version_id", vibePack.versionId());
            o.add("vibe_pack", v);
        } else if (pack != null) {
            JsonObject p = new JsonObject();
            p.addProperty("source", pack.source());
            p.addProperty("project_id", pack.projectId());
            p.addProperty("version_id", pack.versionId());
            if (pack.title() != null) p.addProperty("title", pack.title());
            o.add("pack", p);
        } else {
            o.add("pack", com.google.gson.JsonNull.INSTANCE);
        }
        JsonArray arr = new JsonArray();
        for (Mod m : mods) {
            if (arr.size() >= MAX_MODS) break;
            JsonObject j = new JsonObject();
            j.addProperty("source", m.source());
            j.addProperty("project_id", m.projectId());
            if (m.versionId() != null) j.addProperty("version_id", m.versionId());
            if (m.kind() != null) j.addProperty("kind", m.kind());
            arr.add(j);
        }
        o.add("mods", arr);
        if (clientPack != null) o.addProperty("client_pack", clientPack);
        return o;
    }

    // ============================================================================================ helpers

    private static String str(JsonElement e) {
        if (e == null || !e.isJsonPrimitive()) return null;
        String s = e.getAsString().trim();
        return s.isEmpty() ? null : s;
    }

    private static int num(JsonElement e, int def) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        try {
            return e.getAsInt();
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static String lower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }

    private static String clip(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    /** Ids as the backend accepts them ({@code [A-Za-z0-9._-]{1,64}}), else null. */
    private static String id(String s) {
        return s != null && s.matches("[A-Za-z0-9._-]{1,64}") ? s : null;
    }

    /** The last path segment of a relative file name, or null. */
    private static String fileName(String s) {
        if (s == null) return null;
        String f = s.replace('\\', '/');
        int slash = f.lastIndexOf('/');
        if (slash >= 0) f = f.substring(slash + 1);
        return f.isBlank() ? null : f;
    }
}

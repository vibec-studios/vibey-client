package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class HudShare {
    private HudShare() {}

    public static final String PREFIX = "VHUD1.";
    public static final int MAX_CODE = 24_000;
    public static final int MAX_JSON = 192_000;

    public static final class BadLayout extends Exception {
        public final String key;

        public BadLayout(String key) {
            super(key);
            this.key = key;
        }
    }

    public static String encode(JsonObject json) {
        byte[] raw = json.toString().getBytes(StandardCharsets.UTF_8);
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            d.setInput(raw);
            d.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 2 + 16);
            byte[] buf = new byte[1024];
            while (!d.finished()) out.write(buf, 0, d.deflate(buf));
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        } finally {
            d.end();
        }
    }

    public static JsonObject decode(String code) throws BadLayout {
        if (code == null) throw new BadLayout("core.hud.share.error.empty");
        String c = code.strip();
        if (c.length() >= 2 && (c.charAt(0) == '"' || c.charAt(0) == '\'') && c.charAt(c.length() - 1) == c.charAt(0)) {
            c = c.substring(1, c.length() - 1).strip();
        }
        if (c.isEmpty()) throw new BadLayout("core.hud.share.error.empty");
        if (c.length() > MAX_JSON) throw new BadLayout("core.hud.share.error.tooBig");
        if (c.startsWith("{")) return parseJson(c);
        if (c.regionMatches(true, 0, "VHUD", 0, 4)) {
            int dot = c.indexOf('.');
            if (dot < 0) throw new BadLayout("core.hud.share.error.notCode");
            String ver = c.substring(4, dot);
            if (!ver.equals("1")) throw new BadLayout(ver.matches("\\d{1,3}") ? "core.hud.share.error.newer" : "core.hud.share.error.notCode");
            c = c.substring(dot + 1);
        }
        c = c.replaceAll("\\s+", "");
        if (c.length() > MAX_CODE) throw new BadLayout("core.hud.share.error.tooBig");
        if (!c.matches("[A-Za-z0-9_\\-+/=]+")) throw new BadLayout("core.hud.share.error.notCode");
        byte[] packed;
        try {
            packed = c.indexOf('+') >= 0 || c.indexOf('/') >= 0 ? Base64.getDecoder().decode(c) : Base64.getUrlDecoder().decode(c);
        } catch (IllegalArgumentException e) {
            throw new BadLayout("core.hud.share.error.notCode");
        }
        Inflater inf = new Inflater();
        try {
            inf.setInput(packed);
            ByteArrayOutputStream out = new ByteArrayOutputStream(packed.length * 3);
            byte[] buf = new byte[2048];
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) throw new BadLayout("core.hud.share.error.notCode");
                out.write(buf, 0, n);
                if (out.size() > MAX_JSON) throw new BadLayout("core.hud.share.error.tooBig");
            }
            return parseJson(out.toString(StandardCharsets.UTF_8));
        } catch (DataFormatException e) {
            throw new BadLayout("core.hud.share.error.notCode");
        } finally {
            inf.end();
        }
    }

    static JsonObject parseJson(String text) throws BadLayout {
        try {
            JsonElement el = JsonParser.parseString(text);
            if (!el.isJsonObject()) throw new BadLayout("core.hud.share.error.notLayout");
            JsonObject o = el.getAsJsonObject();
            if (!o.has("e") || !o.get("e").isJsonObject()) throw new BadLayout("core.hud.share.error.notLayout");
            JsonElement v = o.get("v");
            if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() && v.getAsInt() > 1) {
                throw new BadLayout("core.hud.share.error.newer");
            }
            return o;
        } catch (BadLayout b) {
            throw b;
        } catch (RuntimeException | StackOverflowError e) {
            throw new BadLayout("core.hud.share.error.notLayout");
        }
    }

    public static String exportCode() {
        return encode(StateJson.write(HudLayout.capture(), false));
    }

    public static HudLayout.State importCode(String code) throws BadLayout {
        JsonObject json = decode(code);
        try {
            HudLayout.State s = StateJson.parse(json);
            if (s.placements.isEmpty() && s.unknown.isEmpty()) throw new BadLayout("core.hud.share.error.notLayout");
            return s;
        } catch (IllegalArgumentException e) {
            throw new BadLayout("core.hud.share.error.notLayout");
        }
    }

    public static Path folder() {
        return dev.henny.vibeyclient.api.setting.SettingsGroup.configDir().resolve("hud-layouts");
    }

    public static Path exportFile() throws java.io.IOException {
        Path dir = folder();
        Files.createDirectories(dir);
        String name = "layout-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT));
        Path f = dir.resolve(name + ".vhud");
        for (int i = 2; Files.exists(f); i++) f = dir.resolve(name + "-" + i + ".vhud");
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Files.writeString(f, gson.toJson(StateJson.write(HudLayout.capture(), false)), StandardCharsets.UTF_8);
        return f;
    }

    public static HudLayout.State importFile(Path f) throws BadLayout {
        try {
            if (!Files.isRegularFile(f)) throw new BadLayout("core.hud.share.error.noFile");
            if (Files.size(f) > MAX_JSON) throw new BadLayout("core.hud.share.error.tooBig");
            return importCode(Files.readString(f, StandardCharsets.UTF_8));
        } catch (java.io.IOException | java.io.UncheckedIOException e) {
            throw new BadLayout("core.hud.share.error.noFile");
        }
    }

    public static List<Path> files() {
        List<Path> out = new ArrayList<>();
        Path dir = folder();
        if (!Files.isDirectory(dir)) return out;
        try (var s = Files.list(dir)) {
            s.filter(p -> {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return n.endsWith(".vhud") || n.endsWith(".json") || n.endsWith(".txt");
            }).sorted((a, b) -> {
                try {
                    return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                } catch (java.io.IOException e) {
                    return 0;
                }
            }).limit(30).forEach(out::add);
        } catch (java.io.IOException | RuntimeException e) {
        }
        return out;
    }
}

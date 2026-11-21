package dev.henny.vibeyrules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The server-rules protocol (docs/SERVER-RULES.md in vibey-client) and the plugin config, without Bukkit types so it
 * is unit tested. Both channels carry the UTF-8 bytes of one JSON object as the whole plugin-message body.
 */
public final class Rules {
    private Rules() {}

    public static final String RULES_CHANNEL = "vibey:rules";
    public static final String HELLO_CHANNEL = "vibey:hello";
    public static final int SCHEMA = 1;
    public static final int MAX_RULES = 256;
    public static final int MAX_KEY = 64;
    public static final int MAX_MESSAGE = 256;
    public static final int MAX_HELLO_BYTES = 16 * 1024;
    public static final Pattern KEY = Pattern.compile("\\*|[a-z0-9_-]+(\\.[a-z0-9_-]+)*(\\.\\*)?");

    /** What one player gets: disabled keys and the message (null = none). */
    public record Applied(List<String> disable, List<String> allow, String message) {
        public Applied(List<String> disable, String message) {
            this(disable, List.of(), message);
        }

        public static final Applied NONE = new Applied(List.of(), null);

        public boolean isEmpty() {
            return disable.isEmpty() && message == null;
        }
    }

    /** One world override: {@code disable} null = the global list, {@code message} null = the global message. */
    public record World(List<String> disable, List<String> allow, String message) {}

    /** The parsed config.yml. */
    public record Config(boolean enabled, List<String> disable, List<String> allow, String message, Map<String, World> worlds, boolean logHello,
                         List<String> problems) {

        public Applied rulesFor(String world) {
            if (!enabled) return Applied.NONE;
            World w = world == null ? null : worlds.get(world.toLowerCase(Locale.ROOT));
            if (w == null) return new Applied(disable, allow, message);
            return new Applied(w.disable() != null ? w.disable() : disable,
                    w.allow() != null ? w.allow() : allow, w.message() != null ? w.message() : message);
        }
    }

    /** The parsed vibey:hello. */
    public record Hello(String client, String version, List<String> supports) {}

    // ============================================================================================ config

    /**
     * Builds the config from plain values (Bukkit's {@code getBoolean}/{@code getList}/... results). Invalid keys are
     * skipped and reported in {@link Config#problems()}.
     */
    public static Config config(boolean enabled, Collection<?> disable, String message, Map<String, Map<String, Object>> worlds, boolean logHello) {
        return config(enabled, disable, List.of(), message, worlds, logHello);
    }

    public static Config config(boolean enabled, Collection<?> disable, Collection<?> allow, String message,
                                Map<String, Map<String, Object>> worlds, boolean logHello) {
        List<String> problems = new ArrayList<>();
        List<String> global = keys(disable, "disable", problems);
        List<String> exceptions = keys(allow, "allow", problems);
        Map<String, World> w = new LinkedHashMap<>();
        if (worlds != null) {
            for (Map.Entry<String, Map<String, Object>> e : worlds.entrySet()) {
                Map<String, Object> v = e.getValue();
                if (v == null) continue;
                Object m = v.get("message");
                String field = "worlds." + e.getKey() + ".";
                w.put(e.getKey().toLowerCase(Locale.ROOT), new World(
                        worldKeys(v.get("disable"), field + "disable", problems),
                        worldKeys(v.get("allow"), field + "allow", problems),
                        m == null ? null : cleanMessage(String.valueOf(m))));
            }
        }
        return new Config(enabled, global, exceptions, cleanMessage(message), Map.copyOf(w), logHello, List.copyOf(problems));
    }

    private static List<String> worldKeys(Object raw, String field, List<String> problems) {
        if (raw instanceof Collection<?> values) return keys(values, field, problems);
        if (raw != null) problems.add(field + " must be a list");
        return null;
    }

    private static List<String> keys(Collection<?> raw, String field, List<String> problems) {
        if (raw == null) return List.of();
        Set<String> out = new LinkedHashSet<>();
        for (Object o : raw) {
            String k = key(o == null ? null : String.valueOf(o));
            if (k == null) problems.add(field + ": ignoring invalid key '" + o + "'");
            else if (out.size() < MAX_RULES) out.add(k);
        }
        return List.copyOf(out);
    }

    /** Normalised key ({@code hud.*}, {@code zoom}) or null. */
    public static String key(String raw) {
        if (raw == null) return null;
        String k = raw.strip().toLowerCase(Locale.ROOT);
        if (k.isEmpty() || k.length() > MAX_KEY || !KEY.matcher(k).matches()) return null;
        return k;
    }

    /** Strips control characters and colour codes, max {@link #MAX_MESSAGE} chars; null when blank. */
    public static String cleanMessage(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < MAX_MESSAGE; i++) {
            char c = s.charAt(i);
            if (c == '§') {
                i++;
                continue;
            }
            if (c == '\n' || c == '\t') c = ' ';
            if (Character.isISOControl(c)) continue;
            sb.append(c);
        }
        String out = sb.toString().strip();
        return out.isEmpty() ? null : out;
    }

    // ============================================================================================ wire

    public static byte[] encode(Applied rules) {
        JsonObject o = new JsonObject();
        o.addProperty("schema", SCHEMA);
        JsonArray a = new JsonArray();
        for (String k : rules.disable()) a.add(k);
        o.add("disable", a);
        if (!rules.allow().isEmpty()) {
            JsonArray allow = new JsonArray();
            rules.allow().forEach(allow::add);
            o.add("allow", allow);
        }
        if (rules.message() != null) o.addProperty("message", rules.message());
        return o.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** @throws IllegalArgumentException for anything that is not a schema-1 hello */
    public static Hello parseHello(byte[] data) {
        if (data == null || data.length == 0 || data.length > MAX_HELLO_BYTES) throw new IllegalArgumentException("bad size");
        JsonElement e;
        try {
            e = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("not JSON");
        }
        if (!e.isJsonObject()) throw new IllegalArgumentException("not an object");
        JsonObject o = e.getAsJsonObject();
        JsonElement schema = o.get("schema");
        if (schema == null || !schema.isJsonPrimitive() || !schema.getAsJsonPrimitive().isNumber() || schema.getAsDouble() != SCHEMA) {
            throw new IllegalArgumentException("unsupported schema");
        }
        String client = str(o.get("client"));
        if (client == null || !client.matches("[A-Za-z0-9_.+-]{1,64}")) throw new IllegalArgumentException("bad client");
        String version = str(o.get("version"));
        if (version == null) version = "";
        if (version.length() > 64) version = version.substring(0, 64);
        List<String> supports = new ArrayList<>();
        JsonElement s = o.get("supports");
        if (s != null && s.isJsonArray()) {
            for (JsonElement k : s.getAsJsonArray()) {
                String key = key(str(k));
                if (key != null && supports.size() < MAX_RULES && !supports.contains(key)) supports.add(key);
            }
        }
        return new Hello(client, version, List.copyOf(supports));
    }

    private static String str(JsonElement e) {
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    /** Keys of {@code rules} that the client does not know (wildcards count as known if they match one key). */
    public static List<String> unsupported(List<String> rules, List<String> supports) {
        List<String> out = new ArrayList<>();
        for (String r : rules) {
            boolean known;
            if (r.equals("*")) known = !supports.isEmpty();
            else if (r.endsWith(".*")) {
                String prefix = r.substring(0, r.length() - 1);
                known = supports.stream().anyMatch(k -> k.startsWith(prefix));
            } else known = supports.contains(r);
            if (!known) out.add(r);
        }
        return out;
    }
}

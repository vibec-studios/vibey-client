package dev.henny.vibeyclient.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Wire format of the open server-rules channels (contract: vibe-launcher {@code docs/PLAY-LINKS.md} §5, admin docs
 * {@code docs/SERVER-RULES.md}). Both channels carry the UTF-8 bytes of one JSON object as the whole plugin-message
 * body (no length prefix), so any server software can speak them:
 * <ul>
 *   <li>{@code vibey:rules} (server → client): {@code {"schema":1,"disable":["zoom","hud.*"],"message":"..."}}</li>
 *   <li>{@code vibey:hello} (client → server):
 *       {@code {"schema":1,"client":"vibey-client","version":"0.3.0","supports":["zoom",...]}}</li>
 * </ul>
 * Pure Java (Gson only) so the parsing is unit tested and the server side can share it.
 */
public final class RulesProtocol {
    private RulesProtocol() {}

    public static final int SCHEMA = 1;
    public static final String RULES_CHANNEL = "vibey:rules";
    public static final String HELLO_CHANNEL = "vibey:hello";
    /** Bigger packets are dropped (a rules list is a few hundred bytes). */
    public static final int MAX_BYTES = 16 * 1024;
    public static final int MAX_RULES = 256;
    public static final int MAX_KEY = 64;
    public static final int MAX_MESSAGE = 256;
    /** {@code *}, {@code zoom}, {@code hud.cps}, {@code hud.*}: lower-case segments, a wildcard only at the end. */
    public static final Pattern KEY = Pattern.compile("\\*|[a-z0-9_-]+(\\.[a-z0-9_-]+)*(\\.\\*)?");
    private static final Pattern CLIENT = Pattern.compile("[A-Za-z0-9_.+-]{1,64}");

    /** Parsed {@code vibey:rules}: the disabled keys (validated, lower case, no duplicates) and the message or null. */
    public record Rules(List<String> disable, String message) {
        public static final Rules NONE = new Rules(List.of(), null);

        public boolean isEmpty() {
            return disable.isEmpty() && message == null;
        }
    }

    /** Parsed {@code vibey:hello}. */
    public record Hello(String client, String version, List<String> supports) {}

    /** Thrown for packets that are not valid JSON of the expected shape (the receiver logs and ignores them). */
    public static final class BadPacket extends Exception {
        public BadPacket(String message) {
            super(message);
        }
    }

    // ============================================================================================ rules

    public static Rules parseRules(byte[] data) throws BadPacket {
        JsonObject o = object(data);
        checkSchema(o);
        List<String> disable = new ArrayList<>();
        JsonElement d = o.get("disable");
        if (d != null && !d.isJsonNull()) {
            if (!d.isJsonArray()) throw new BadPacket("\"disable\" must be an array");
            Set<String> seen = new LinkedHashSet<>();
            for (JsonElement e : d.getAsJsonArray()) {
                if (seen.size() >= MAX_RULES) break;
                String k = key(e);
                if (k != null) seen.add(k);
            }
            disable.addAll(seen);
        }
        return new Rules(List.copyOf(disable), message(o.get("message")));
    }

    /** One rule entry, normalised; null if it is not a valid key (skipped, the rest of the list still counts). */
    public static String key(JsonElement e) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) return null;
        return key(e.getAsString());
    }

    /** Normalised key or null if invalid. */
    public static String key(String raw) {
        if (raw == null) return null;
        String k = raw.strip().toLowerCase(Locale.ROOT);
        if (k.isEmpty() || k.length() > MAX_KEY || !KEY.matcher(k).matches()) return null;
        return k;
    }

    private static String message(JsonElement m) throws BadPacket {
        if (m == null || m.isJsonNull()) return null;
        if (!m.isJsonPrimitive() || !m.getAsJsonPrimitive().isString()) throw new BadPacket("\"message\" must be a string");
        return cleanMessage(m.getAsString());
    }

    /** Strips control characters and section-sign colour codes, trims to {@link #MAX_MESSAGE}; null when blank. */
    public static String cleanMessage(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(Math.min(s.length(), MAX_MESSAGE + 8));
        for (int i = 0; i < s.length() && sb.length() < MAX_MESSAGE; i++) {
            char c = s.charAt(i);
            if (c == '§') {
                i++; // skip the colour code character too
                continue;
            }
            if (c == '\n' || c == '\t') c = ' ';
            if (Character.isISOControl(c)) continue;
            sb.append(c);
        }
        String out = sb.toString().strip();
        return out.isEmpty() ? null : out;
    }

    public static byte[] encodeRules(Rules rules) {
        JsonObject o = new JsonObject();
        o.addProperty("schema", SCHEMA);
        JsonArray a = new JsonArray();
        for (String k : rules.disable()) a.add(k);
        o.add("disable", a);
        if (rules.message() != null) o.addProperty("message", rules.message());
        return o.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ============================================================================================ hello

    public static Hello parseHello(byte[] data) throws BadPacket {
        JsonObject o = object(data);
        checkSchema(o);
        String client = string(o.get("client"));
        if (client == null || !CLIENT.matcher(client).matches()) throw new BadPacket("bad \"client\"");
        String version = string(o.get("version"));
        if (version != null && version.length() > 64) version = version.substring(0, 64);
        List<String> supports = new ArrayList<>();
        JsonElement s = o.get("supports");
        if (s != null && s.isJsonArray()) {
            for (JsonElement e : s.getAsJsonArray()) {
                if (supports.size() >= MAX_RULES) break;
                String k = key(e);
                if (k != null && !supports.contains(k)) supports.add(k);
            }
        }
        return new Hello(client, version == null ? "" : version, List.copyOf(supports));
    }

    public static byte[] encodeHello(String client, String version, List<String> supports, String minecraft, String loader) {
        JsonObject o = new JsonObject();
        o.addProperty("schema", SCHEMA);
        o.addProperty("client", client);
        o.addProperty("version", version);
        JsonArray a = new JsonArray();
        for (String k : supports) {
            if (a.size() >= MAX_RULES) break;
            a.add(k);
        }
        o.add("supports", a);
        // extra fields: receivers ignore what they do not know
        if (minecraft != null) o.addProperty("minecraft", minecraft);
        if (loader != null) o.addProperty("loader", loader);
        return o.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ============================================================================================ helpers

    private static JsonObject object(byte[] data) throws BadPacket {
        if (data == null) throw new BadPacket("too big (over " + MAX_BYTES + " bytes)");
        if (data.length == 0) throw new BadPacket("empty");
        if (data.length > MAX_BYTES) throw new BadPacket("too big (" + data.length + " bytes)");
        JsonElement e;
        try {
            e = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
        } catch (RuntimeException ex) {
            throw new BadPacket("not JSON");
        }
        if (!e.isJsonObject()) throw new BadPacket("not a JSON object");
        return e.getAsJsonObject();
    }

    private static void checkSchema(JsonObject o) throws BadPacket {
        JsonElement s = o.get("schema");
        if (s == null || !s.isJsonPrimitive() || !s.getAsJsonPrimitive().isNumber()) throw new BadPacket("missing \"schema\"");
        JsonPrimitive p = s.getAsJsonPrimitive();
        if (p.getAsDouble() != SCHEMA) throw new BadPacket("unsupported schema " + p.getAsString());
    }

    private static String string(JsonElement e) {
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }
}

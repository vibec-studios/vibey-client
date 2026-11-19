package dev.henny.vibeyclient.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * {@code config/vibeyclient-server.json} of a Fabric/NeoForge server (same options as the Paper plugin's
 * {@code config.yml}, see {@code docs/SERVER-RULES.md}). A world override replaces the global {@code disable} list
 * (and the message, if it sets one) for players in that dimension. Pure Java, unit tested.
 */
public record ServerRulesConfig(boolean enabled, List<String> disable, String message, Map<String, World> worlds,
                                Set<String> bypass, boolean logHello) {

    public static final ServerRulesConfig EMPTY = new ServerRulesConfig(true, List.of(), null, Map.of(), Set.of(), true);

    /** Override for one dimension ({@code minecraft:the_nether}); {@code message} null = the global one. */
    public record World(List<String> disable, String message) {}

    public record Loaded(ServerRulesConfig config, String error) {}

    static final String DEFAULT = """
            {
              "_help": "Vibey server rules: features listed in 'disable' are switched off for players with Vibey Client while they are on this server. Keys and examples: https://github.com/vibec-studios/vibey-client/blob/main/docs/SERVER-RULES.md . Reload with /vibeyrules reload.",
              "_example": { "disable": ["zoom", "freelook", "hud.coords", "minimap"], "message": "Zoom, freelook and coordinates are off on this server.", "worlds": { "minecraft:the_nether": { "disable": ["hud.*"] } }, "bypass": ["StaffName", "00000000-0000-0000-0000-000000000000"] },
              "enabled": true,
              "disable": [],
              "message": "",
              "worlds": {},
              "bypass": [],
              "logHello": true
            }
            """;

    /** The rules for one player in one dimension. */
    public RulesProtocol.Rules rulesFor(String world, UUID uuid, String name) {
        if (!enabled) return RulesProtocol.Rules.NONE;
        if (uuid != null && bypass.contains(uuid.toString())) return RulesProtocol.Rules.NONE;
        if (name != null && bypass.contains(name.toLowerCase(Locale.ROOT))) return RulesProtocol.Rules.NONE;
        World w = world == null ? null : worlds.get(world);
        if (w != null) return new RulesProtocol.Rules(w.disable(), w.message() != null ? w.message() : message);
        return new RulesProtocol.Rules(disable, message);
    }

    /** Reads the file; writes the default when it does not exist. Bad files give {@link #EMPTY} plus an error. */
    public static Loaded load(Path file) {
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT, StandardCharsets.UTF_8);
            }
            return new Loaded(parse(Files.readString(file, StandardCharsets.UTF_8)), null);
        } catch (IOException e) {
            return new Loaded(EMPTY, "could not read " + file.getFileName() + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return new Loaded(EMPTY, file.getFileName() + " is invalid, no rules active: " + e.getMessage());
        }
    }

    /** @throws IllegalArgumentException for broken JSON or wrong types */
    public static ServerRulesConfig parse(String json) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("not valid JSON (" + e.getMessage() + ")");
        }
        if (!root.isJsonObject()) throw new IllegalArgumentException("must be a JSON object");
        JsonObject o = root.getAsJsonObject();
        boolean enabled = bool(o, "enabled", true);
        List<String> disable = keys(o.get("disable"), "disable");
        String message = RulesProtocol.cleanMessage(str(o.get("message"), "message"));
        Map<String, World> worlds = new LinkedHashMap<>();
        JsonElement w = o.get("worlds");
        if (w != null && !w.isJsonNull()) {
            if (!w.isJsonObject()) throw new IllegalArgumentException("'worlds' must be an object");
            for (Map.Entry<String, JsonElement> e : w.getAsJsonObject().entrySet()) {
                if (!e.getValue().isJsonObject()) throw new IllegalArgumentException("'worlds." + e.getKey() + "' must be an object");
                JsonObject wo = e.getValue().getAsJsonObject();
                String id = e.getKey().strip().toLowerCase(Locale.ROOT);
                if (!id.contains(":")) id = "minecraft:" + id;
                worlds.put(id, new World(wo.has("disable") ? keys(wo.get("disable"), "worlds." + e.getKey() + ".disable") : disable,
                        RulesProtocol.cleanMessage(str(wo.get("message"), "worlds." + e.getKey() + ".message"))));
            }
        }
        Set<String> bypass = new LinkedHashSet<>();
        JsonElement b = o.get("bypass");
        if (b != null && !b.isJsonNull()) {
            if (!b.isJsonArray()) throw new IllegalArgumentException("'bypass' must be a list");
            for (JsonElement e : b.getAsJsonArray()) {
                String s = str(e, "bypass");
                if (s != null && !s.isBlank()) bypass.add(s.strip().toLowerCase(Locale.ROOT));
            }
        }
        return new ServerRulesConfig(enabled, disable, message, Map.copyOf(worlds), Set.copyOf(bypass), bool(o, "logHello", true));
    }

    private static List<String> keys(JsonElement e, String field) {
        if (e == null || e.isJsonNull()) return List.of();
        if (!e.isJsonArray()) throw new IllegalArgumentException("'" + field + "' must be a list");
        Set<String> out = new LinkedHashSet<>();
        JsonArray a = e.getAsJsonArray();
        for (JsonElement k : a) {
            String key = RulesProtocol.key(k);
            if (key == null) throw new IllegalArgumentException("'" + field + "' has an invalid key " + k);
            if (out.size() < RulesProtocol.MAX_RULES) out.add(key);
        }
        return List.copyOf(new ArrayList<>(out));
    }

    private static String str(JsonElement e, String field) {
        if (e == null || e.isJsonNull()) return null;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("'" + field + "' must be text");
        return e.getAsString();
    }

    private static boolean bool(JsonObject o, String field, boolean def) {
        JsonElement e = o.get(field);
        if (e == null || e.isJsonNull()) return def;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("'" + field + "' must be true or false");
        return e.getAsBoolean();
    }
}

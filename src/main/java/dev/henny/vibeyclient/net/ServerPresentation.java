package dev.henny.vibeyclient.net;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ServerPresentation(List<Waypoint> waypoints, List<Team> teams, Map<UUID, Integer> nametagColors) {
    public static final String CHANNEL = "vibey:presentation";
    public static final int MAX_BYTES = 16 * 1024;
    public static final ServerPresentation EMPTY = new ServerPresentation(List.of(), List.of(), Map.of());
    private static final Gson GSON = new Gson();

    public ServerPresentation {
        waypoints = List.copyOf(waypoints);
        teams = List.copyOf(teams);
        nametagColors = Map.copyOf(nametagColors);
        if (waypoints.size() > 64 || teams.size() > 32 || nametagColors.size() > 256) {
            throw new IllegalArgumentException("Too many presentation entries");
        }
        Set<String> waypointIds = new HashSet<>();
        for (Waypoint waypoint : waypoints) {
            if (!waypointIds.add(waypoint.id())) throw new IllegalArgumentException("Duplicate waypoint ID");
        }
        Set<String> teamIds = new HashSet<>();
        Set<UUID> members = new HashSet<>();
        for (Team team : teams) {
            if (!teamIds.add(team.id())) throw new IllegalArgumentException("Duplicate team ID");
            for (UUID member : team.players()) {
                if (!members.add(member)) throw new IllegalArgumentException("Duplicate team member");
            }
        }
        if (members.size() > 256) throw new IllegalArgumentException("Too many team members");
        nametagColors.values().forEach(ServerPresentation::checkColor);
    }

    public record Waypoint(String id, String label, String dimension, int x, int y, int z, int color) {
        public Waypoint {
            checkId(id);
            checkLabel(label);
            if (dimension == null || dimension.length() > 128 || !dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("Invalid dimension");
            }
            if (Math.abs((long) x) > 30_000_000 || Math.abs((long) z) > 30_000_000 || Math.abs((long) y) > 4096) {
                throw new IllegalArgumentException("Invalid waypoint position");
            }
            checkColor(color);
        }
    }

    public record Team(String id, String label, int color, List<UUID> players) {
        public Team {
            checkId(id);
            checkLabel(label);
            checkColor(color);
            players = List.copyOf(players);
        }
    }

    public Map<UUID, Integer> resolvedColors() {
        Map<UUID, Integer> colors = new HashMap<>();
        for (Team team : teams) {
            for (UUID uuid : team.players()) colors.put(uuid, team.color());
        }
        colors.putAll(nametagColors);
        return Map.copyOf(colors);
    }

    public byte[] encode() {
        JsonObject json = new JsonObject();
        json.addProperty("schema", 1);
        json.add("waypoints", GSON.toJsonTree(waypoints));
        json.add("teams", GSON.toJsonTree(teams));
        json.add("nametagColors", GSON.toJsonTree(nametagColors));
        byte[] encoded = json.toString().getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_BYTES) throw new IllegalArgumentException("Presentation exceeds 16 KiB");
        return encoded;
    }

    public static ServerPresentation decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("Invalid presentation packet size");
        }
        try {
            JsonObject json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (integer(json.get("schema")) != 1) throw new IllegalArgumentException("Unsupported presentation schema");
            List<Waypoint> waypoints = new ArrayList<>();
            for (JsonElement element : array(json, "waypoints")) {
                JsonObject entry = element.getAsJsonObject();
                waypoints.add(new Waypoint(string(entry.get("id")), string(entry.get("label")), string(entry.get("dimension")),
                        integer(entry.get("x")), integer(entry.get("y")), integer(entry.get("z")), integer(entry.get("color"))));
            }
            List<Team> teams = new ArrayList<>();
            for (JsonElement element : array(json, "teams")) {
                JsonObject entry = element.getAsJsonObject();
                List<UUID> players = new ArrayList<>();
                for (JsonElement player : array(entry, "players")) players.add(uuid(string(player)));
                teams.add(new Team(string(entry.get("id")), string(entry.get("label")), integer(entry.get("color")), players));
            }
            Map<UUID, Integer> colors = new HashMap<>();
            if (json.has("nametagColors")) {
                json.getAsJsonObject("nametagColors").entrySet().forEach(e -> colors.put(uuid(e.getKey()), integer(e.getValue())));
            }
            return new ServerPresentation(waypoints, teams, colors);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid presentation packet", e);
        }
    }

    private static UUID uuid(String value) {
        UUID parsed = UUID.fromString(value);
        if (!parsed.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException("Invalid UUID");
        return parsed;
    }

    private static JsonArray array(JsonObject json, String key) {
        return json.has(key) ? json.getAsJsonArray(key) : new JsonArray();
    }

    private static String string(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected a string");
        }
        return value.getAsString();
    }

    private static int integer(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected an integer");
        }
        return value.getAsBigDecimal().intValueExact();
    }

    private static void checkId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]{1,48}")) throw new IllegalArgumentException("Invalid ID");
    }

    private static void checkLabel(String label) {
        if (label == null || label.isBlank() || label.length() > 64
                || label.codePoints().anyMatch(c -> Character.isISOControl(c) || c == 0xA7 || Character.getType(c) == Character.FORMAT)) {
            throw new IllegalArgumentException("Invalid label");
        }
    }

    private static void checkColor(int color) {
        if (color < 0 || color > 0xFFFFFF) throw new IllegalArgumentException("Invalid RGB color");
    }
}

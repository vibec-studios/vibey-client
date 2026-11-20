package dev.henny.vibeyclient.module.waypoints.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WaypointJson {
    public static final int FORMAT = 1;

    private WaypointJson() {}

    public static String write(List<Waypoint> list) {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        JsonArray arr = new JsonArray();
        for (Waypoint w : list) arr.add(write(w));
        root.add("waypoints", arr);
        return root.toString();
    }

    public static JsonObject write(Waypoint w) {
        JsonObject o = new JsonObject();
        o.addProperty("id", w.id());
        o.addProperty("name", w.name());
        o.addProperty("x", w.pos().x());
        o.addProperty("y", w.pos().y());
        o.addProperty("z", w.pos().z());
        o.addProperty("dim", w.dimension());
        o.addProperty("color", Colors.hex(w.color()));
        o.addProperty("size", w.size());
        o.addProperty("visible", w.visible());
        o.addProperty("kind", w.kind().name().toLowerCase(Locale.ROOT));
        o.addProperty("created", w.created());
        return o;
    }

    public static List<Waypoint> read(String json) {
        List<Waypoint> out = new ArrayList<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            return out;
        }
        if (!root.isJsonObject() || !root.getAsJsonObject().has("waypoints")) return out;
        JsonElement arr = root.getAsJsonObject().get("waypoints");
        if (!arr.isJsonArray()) return out;
        for (JsonElement e : arr.getAsJsonArray()) {
            Waypoint w = readOne(e);
            if (w != null && out.size() < WaypointList.MAX && out.stream().noneMatch(o -> o.id().equals(w.id()))) out.add(w);
        }
        return out;
    }

    static Waypoint readOne(JsonElement e) {
        try {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            String dim = o.get("dim").getAsString();
            long x = o.get("x").getAsLong(), y = o.get("y").getAsLong(), z = o.get("z").getAsLong();
            if (!id.matches("[A-Za-z0-9_-]{1,40}") || !Dimensions.valid(dim) || !Pos.valid(x, y, z)) return null;
            Waypoint w = new Waypoint(id, o.get("name").getAsString(), new Pos((int) x, (int) y, (int) z), dim,
                    Colors.parseHex(o.get("color").getAsString()));
            if (o.has("size")) w.size(o.get("size").getAsFloat());
            if (o.has("visible")) w.visible(o.get("visible").getAsBoolean());
            if (o.has("kind") && "death".equals(o.get("kind").getAsString())) w.kind(WaypointKind.DEATH);
            if (o.has("created")) w.created(o.get("created").getAsLong());
            return w;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}

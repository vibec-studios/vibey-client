package dev.henny.vibeyclient.client.ranks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.henny.vibeyclient.client.VibeyPlayers;
import dev.henny.vibeyclient.ranks.IdentityJson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerEntry {
    private PlayerEntry() {}

    public static boolean carriesNametag(JsonObject d) {
        return d != null && (d.has("client") || d.has("badges") || IdentityJson.hasRanks(d));
    }

    public static void apply(UUID id, JsonObject entry) {
        if (id == null || entry == null) return;
        boolean vibey = entry.get("client") instanceof JsonPrimitive c && "vibey".equals(c.getAsString());
        VibeyPlayers.backend(id, vibey, badges(entry.get("badges")), IdentityJson.parse(entry));
    }

    public static UUID uuid(JsonElement e) {
        if (!(e instanceof JsonPrimitive p) || !p.isString()) return null;
        try {
            return UUID.fromString(p.getAsString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static List<String> badges(JsonElement e) {
        if (!(e instanceof JsonArray a)) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonElement x : a) if (x instanceof JsonPrimitive p && p.isString()) out.add(p.getAsString());
        return out;
    }
}

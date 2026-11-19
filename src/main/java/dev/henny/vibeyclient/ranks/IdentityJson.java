package dev.henny.vibeyclient.ranks;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;

public final class IdentityJson {
    private IdentityJson() {}

    public static boolean hasRanks(JsonObject o) {
        return o != null && (o.has("rank") || o.has("nameStyle"));
    }

    public static Identity parse(JsonObject o) {
        if (o == null) return Identity.EMPTY;
        if (!hasRanks(o)) return LegacyBadges.identity(legacyBadges(o));
        String rank = o.get("rank") instanceof JsonPrimitive p && p.isString() ? clean(p.getAsString()) : null;
        List<String> tags = Json.strings(o.get("tags")).stream().map(IdentityJson::clean)
                .filter(t -> t != null && !t.equals(rank)).distinct().limit(1).toList();
        String style = clean(Json.text(Json.obj(o, "nameStyle"), "id", null));
        Identity id = new Identity(rank, tags, style);
        return id.isEmpty() ? Identity.EMPTY : id;
    }

    private static List<String> legacyBadges(JsonObject o) {
        if (o.has("badges")) return Json.strings(o.get("badges"));
        String badge = Json.text(o, "badge", null);
        return badge == null ? List.of() : List.of(badge);
    }

    public static String clean(String id) {
        if (id == null || id.isEmpty() || id.length() > 32) return null;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-';
            if (!ok) return null;
        }
        return id;
    }
}

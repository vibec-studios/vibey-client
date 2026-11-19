package dev.henny.vibeyclient.ranks;

import com.google.gson.JsonObject;

import java.util.List;

public record OwnRanks(List<String> held, String primary, List<String> styles, boolean showRank, String style) {
    public static final OwnRanks NONE = new OwnRanks(List.of(), null, List.of(), true, null);

    public static OwnRanks fromMe(JsonObject me) {
        if (me == null) return NONE;
        JsonObject r = Json.obj(me, "ranks");
        boolean show = Json.bool(Json.obj(me, "privacy"), "showRank", true);
        String style = Json.text(Json.obj(me, "nameStyle"), "id", null);
        return new OwnRanks(Json.strings(r == null ? null : r.get("held")), Json.text(r, "primary", null),
                Json.strings(r == null ? null : r.get("styles")), show, style);
    }

    public boolean known() {
        return this != NONE;
    }
}

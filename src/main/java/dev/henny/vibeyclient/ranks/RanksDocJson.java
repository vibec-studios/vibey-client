package dev.henny.vibeyclient.ranks;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.ranks.style.ColorMath;
import dev.henny.vibeyclient.ranks.style.NameStyle;
import dev.henny.vibeyclient.ranks.style.NameStyleJson;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RanksDocJson {
    private RanksDocJson() {}

    public static RanksDoc parse(JsonObject o) {
        if (o == null || !o.has("ranks")) return null;
        Map<String, RankDef> ranks = new LinkedHashMap<>();
        for (JsonObject r : Json.objects(o.get("ranks"))) {
            RankDef d = rank(r);
            if (d != null) ranks.put(d.id(), d);
        }
        List<NameStyleDef> styles = new ArrayList<>();
        for (JsonObject s : Json.objects(o.get("nameStyles"))) {
            NameStyleDef d = style(s);
            if (d != null) styles.add(d);
        }
        styles.sort(Comparator.comparingInt(NameStyleDef::sort));
        return new RanksDoc(Json.text(o, "version", ""), Map.copyOf(ranks), List.copyOf(styles));
    }

    static RankDef rank(JsonObject r) {
        String id = IdentityJson.clean(Json.text(r, "id", null));
        if (id == null) return null;
        return new RankDef(id, Label.of(r.get("label"), id), Json.text(r, "icon", ""), ColorMath.parseHex(Json.text(r, "color", "#ffffff")),
                Json.num(r, "priority"), Json.text(r, "kind", "both"), Json.bool(r, "staff", false));
    }

    static NameStyleDef style(JsonObject s) {
        String id = IdentityJson.clean(Json.text(s, "id", null));
        NameStyle style = NameStyleJson.parse(s);
        if (id == null || style == null) return null;
        return new NameStyleDef(id, Label.of(s.get("label"), id), Json.strings(s.get("ranks")), Json.num(s, "sort"),
                Json.text(s, "effect", ""), style);
    }
}

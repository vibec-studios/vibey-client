package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.BundleHeader;
import dev.henny.cosmetico.model.Kind;
import dev.henny.cosmetico.model.Rarity;
import dev.henny.cosmetico.model.Text;
import dev.henny.cosmetico.version.VersionGate;

import java.util.ArrayList;
import java.util.List;

import static dev.henny.cosmetico.parse.Json.num;
import static dev.henny.cosmetico.parse.Json.str;

final class HeaderReader {
    private HeaderReader() {}

    static BundleHeader read(JsonObject o, VersionGate gate) {
        if (!"vibey".equals(str(o, "format"))) throw new BundleException("format must be \"vibey\"");
        double fv = num(o, "formatVersion", -1);
        String minClient = str(o, "minClientVersion");
        if (minClient != null && !gate.satisfies(minClient))
            throw new NeedsUpdateException(minClient, "needs Vibey Client " + minClient + " or newer");
        if (fv > 1) throw new NeedsUpdateException(null, "formatVersion " + (long) fv + " needs a newer Vibey Client");
        if (fv != 1) throw new BundleException("formatVersion must be 1");
        String kindS = str(o, "kind");
        Kind kind = "cosmetic".equals(kindS) ? Kind.COSMETIC : "emote".equals(kindS) ? Kind.EMOTE : null;
        if (kind == null) throw new BundleException("kind must be cosmetic or emote");
        String id = str(o, "id");
        if (id == null || id.length() > Limits.MAX_ID || !Limits.ID.matcher(id).matches())
            throw new BundleException("id \"" + id + "\" is not a valid namespace:path id");
        Text name = Json.text(o.get("name"));
        if (name == null || name.en() == null || name.en().isEmpty()) throw new BundleException("name.en is required");
        Text description = Json.text(o.get("description"));
        String author = str(o, "author");
        List<String> tags = new ArrayList<>();
        if (o.get("tags") instanceof JsonArray a) for (JsonElement e : a) if (e.isJsonPrimitive()) tags.add(e.getAsString());
        Rarity rarity = Rarity.of(str(o, "rarity"));
        return new BundleHeader(id, kind, name, description == null ? Text.EMPTY : description, author == null ? "" : author,
                List.copyOf(tags), rarity);
    }
}

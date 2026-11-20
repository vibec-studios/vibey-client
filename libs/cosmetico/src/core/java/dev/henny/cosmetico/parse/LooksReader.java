package dev.henny.cosmetico.parse;

import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.BundleLooks;
import dev.henny.cosmetico.model.EmoteOptions;
import dev.henny.cosmetico.model.Kind;

import java.util.EnumSet;
import java.util.List;

import static dev.henny.cosmetico.parse.Json.bool;
import static dev.henny.cosmetico.parse.Json.str;

final class LooksReader {
    private LooksReader() {}

    static BundleLooks read(JsonObject o, Kind kind, Assets assets, List<String> warnings) {
        if (kind == Kind.COSMETIC) return CosmeticReader.read(o, assets, warnings);
        if (!(o.get("emote") instanceof JsonObject e)) throw new BundleException("emote section is required");
        String anim = str(e, "animation");
        if (anim == null) anim = "main";
        EmoteOptions emote = new EmoteOptions(anim, bool(e, "loop", false), bool(e, "thirdPerson", true), bool(e, "stopOnMove", true),
                bool(e, "hideHeldItems", true));
        return new BundleLooks(null, java.util.Collections.unmodifiableSet(EnumSet.noneOf(dev.henny.cosmetico.model.Armor.class)),
                null, -1, -1, emote, List.of());
    }
}

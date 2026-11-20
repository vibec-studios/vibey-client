package dev.henny.companio.parse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.def.AnimRef;
import dev.henny.companio.def.Blend;

final class AnimRefReader {
    private AnimRefReader() {}

    static AnimRef read(JsonElement e, String at, ReadContext c, Blend defaultBlend) {
        if (e == null || e.isJsonNull()) return null;
        if (Json.isString(e)) return check(new AnimRef(e.getAsString(), defaultBlend, 0.15f, 1f), at, c);
        if (!e.isJsonObject()) {
            c.problems().error(at, "must be an animation name or { \"name\": …, \"blend\": \"add\" }");
            return null;
        }
        return fields(e.getAsJsonObject(), at, c, defaultBlend);
    }

    static AnimRef fields(JsonObject o, String at, ReadContext c, Blend defaultBlend) {
        String name = Json.str(o, "name", at, c.problems(), null);
        if (name == null) {
            c.problems().error(Json.at(at, "name"), "needs the name of a model animation");
            return null;
        }
        String blendId = Json.str(o, "blend", at, c.problems(), null);
        Blend blend = blendId == null ? defaultBlend : Blend.of(blendId);
        if (blend == null) {
            c.problems().error(Json.at(at, "blend"), "must be \"add\" or \"replace\"");
            blend = defaultBlend;
        }
        float fade = Json.num(o, "fade", at, c.problems(), 0.15f, 0f, 5f);
        float speed = Json.num(o, "speed", at, c.problems(), 1f, 0.05f, 10f);
        return check(new AnimRef(name, blend, fade, speed), at, c);
    }

    private static AnimRef check(AnimRef r, String at, ReadContext c) {
        if (c.hasAnimation(r.name())) return r;
        c.problems().error(at, "the model has no animation \"" + r.name() + "\" (" + c.animations() + ")");
        return null;
    }
}

package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.OffsetRule;
import dev.henny.cosmetico.model.Slot;

import java.util.ArrayList;
import java.util.List;

import static dev.henny.cosmetico.parse.Json.str;

final class OffsetWhenReader {
    private OffsetWhenReader() {}

    static List<OffsetRule> read(JsonElement e, Slot own, List<String> warnings) {
        if (e == null || e.isJsonNull()) return List.of();
        if (!(e instanceof JsonArray a)) {
            warnings.add("cosmetic.offsetWhen must be an array (ignored)");
            return List.of();
        }
        List<OffsetRule> out = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            String where = "cosmetic.offsetWhen[" + i + "]";
            if (out.size() >= Limits.MAX_OFFSET_RULES) {
                warnings.add("cosmetic.offsetWhen: more than " + Limits.MAX_OFFSET_RULES + " rules, the rest is ignored");
                break;
            }
            OffsetRule r = rule(a.get(i), where, own, warnings);
            if (r != null) out.add(r);
        }
        return List.copyOf(out);
    }

    private static OffsetRule rule(JsonElement el, String where, Slot own, List<String> warnings) {
        if (!(el instanceof JsonObject r)) {
            warnings.add(where + " must be an object (ignored)");
            return null;
        }
        Slot slot = Slot.of(str(r, "slot"));
        if (slot == null) {
            warnings.add(where + ": unknown slot \"" + str(r, "slot") + "\" (ignored)");
            return null;
        }
        if (slot == own) {
            warnings.add(where + ": refers to the item's own slot (ignored)");
            return null;
        }
        try {
            float[] off = r.has("offset") ? Json.floats(r.get("offset"), 3, where + ".offset") : new float[3];
            float[] rot = r.has("rotation") ? Json.floats(r.get("rotation"), 3, where + ".rotation") : new float[3];
            for (int k = 0; k < 3; k++) {
                if (Math.abs(off[k]) > Limits.MAX_OFFSET) throw new BundleException(where + ".offset must be within ±" + (int) Limits.MAX_OFFSET + " px");
                if (Math.abs(rot[k]) > Limits.MAX_OFFSET_ROTATION)
                    throw new BundleException(where + ".rotation must be within ±" + (int) Limits.MAX_OFFSET_ROTATION + "°");
            }
            return new OffsetRule(slot, off, rot);
        } catch (BundleException ex) {
            warnings.add(ex.getMessage() + " (ignored)");
            return null;
        }
    }
}

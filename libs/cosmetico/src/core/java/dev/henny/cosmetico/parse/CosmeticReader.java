package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Armor;
import dev.henny.cosmetico.model.BundleLooks;
import dev.henny.cosmetico.model.OffsetRule;
import dev.henny.cosmetico.model.PetFollow;
import dev.henny.cosmetico.model.PetOptions;
import dev.henny.cosmetico.model.Slot;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static dev.henny.cosmetico.parse.Json.num;
import static dev.henny.cosmetico.parse.Json.str;

final class CosmeticReader {
    private CosmeticReader() {}

    static BundleLooks read(JsonObject o, Assets assets, List<String> warnings) {
        if (!(o.get("cosmetic") instanceof JsonObject c)) throw new BundleException("cosmetic section is required");
        Slot slot = Slot.of(str(c, "slot"));
        if (slot == null) throw new BundleException("cosmetic.slot \"" + str(c, "slot") + "\" is unknown");
        Set<Armor> hide = hideWithArmor(c, warnings);
        JsonObject p = c.get("pet") instanceof JsonObject po ? po : new JsonObject();
        float scale = (float) num(p, "scale", 1);
        if (!(scale > 0) || scale > 8) throw new BundleException("cosmetic.pet.scale must be a positive number");
        PetOptions pet = new PetOptions(PetFollow.of(str(p, "follow")), scale);
        List<OffsetRule> offsetWhen = OffsetWhenReader.read(c.get("offsetWhen"), slot, warnings);
        int cape = slot == Slot.CAPE ? flatTexture(o, c, "cape", assets) : -1;
        int shield = slot == Slot.SHIELD ? flatTexture(o, c, "shield", assets) : -1;
        if (slot != Slot.CAPE && slot != Slot.SHIELD && !(o.get("model") instanceof JsonObject))
            throw new BundleException("model is required for slot " + slot.id());
        return new BundleLooks(slot, Collections.unmodifiableSet(hide), pet, cape, shield, null, offsetWhen);
    }

    private static Set<Armor> hideWithArmor(JsonObject c, List<String> warnings) {
        Set<Armor> hide = EnumSet.noneOf(Armor.class);
        if (!(c.get("hideWithArmor") instanceof JsonArray ha)) return hide;
        for (JsonElement e : ha) {
            try {
                hide.add(Armor.valueOf(e.getAsString().toUpperCase(Locale.ROOT)));
            } catch (RuntimeException ex) {
                warnings.add("hideWithArmor: unknown armor slot " + e);
            }
        }
        return hide;
    }

    private static int flatTexture(JsonObject o, JsonObject c, String key, Assets assets) {
        String tid = c.get(key) instanceof JsonObject so ? str(so, "texture") : null;
        Integer ti = tid == null ? null : assets.texture(tid);
        if (ti != null) return ti;
        if (o.get("model") == null) throw new BundleException("cosmetic." + key + ".texture \"" + tid + "\" does not exist");
        return -1;
    }
}

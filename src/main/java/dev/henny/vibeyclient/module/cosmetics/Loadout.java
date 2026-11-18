package dev.henny.vibeyclient.module.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.Wardrobe;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** The local player's loadout: item id per slot and the 8 emote wheel entries (SPEC §7 Loadout). */
public final class Loadout {
    public final String[] slots = new String[Slot.all().length];
    public final String[] wheel = new String[Wardrobe.WHEEL_SLOTS];

    public Loadout copy() {
        Loadout l = new Loadout();
        System.arraycopy(slots, 0, l.slots, 0, slots.length);
        System.arraycopy(wheel, 0, l.wheel, 0, wheel.length);
        return l;
    }

    public String get(Slot s) {
        return slots[s.ordinal()];
    }

    /** {@code slot id → item id} for every non-empty slot. */
    public Map<String, String> slotMap() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Slot s : Slot.all()) if (slots[s.ordinal()] != null) m.put(s.id(), slots[s.ordinal()]);
        return m;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonObject s = new JsonObject();
        for (Slot slot : Slot.all()) {
            String id = slots[slot.ordinal()];
            s.add(slot.id(), id == null ? JsonNull.INSTANCE : new JsonPrimitive(id));
        }
        o.add("slots", s);
        JsonArray w = new JsonArray();
        for (String id : wheel) w.add(id == null ? JsonNull.INSTANCE : new JsonPrimitive(id));
        o.add("emoteWheel", w);
        return o;
    }

    public static Loadout fromJson(JsonElement e) {
        Loadout l = new Loadout();
        if (!(e instanceof JsonObject o)) return l;
        if (o.get("slots") instanceof JsonObject s) {
            for (Map.Entry<String, JsonElement> en : s.entrySet()) {
                Slot slot = Slot.of(en.getKey());
                if (slot != null && en.getValue().isJsonPrimitive()) l.slots[slot.ordinal()] = en.getValue().getAsString();
            }
        }
        if (o.get("emoteWheel") instanceof JsonArray w) {
            for (int i = 0; i < Math.min(w.size(), l.wheel.length); i++) {
                if (w.get(i).isJsonPrimitive()) l.wheel[i] = w.get(i).getAsString();
            }
        }
        return l;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Loadout l && Arrays.equals(slots, l.slots) && Arrays.equals(wheel, l.wheel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(slots), Arrays.hashCode(wheel));
    }
}

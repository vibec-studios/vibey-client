package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.api.setting.SettingsGroup;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

final class HudFile {
    private HudFile() {}

    private static final Map<String, SettingsGroup> GROUPS = new java.util.HashMap<>();
    private static SettingsGroup FILE = group(HudProfiles.MAIN.file());
    private static volatile boolean loaded;

    private static SettingsGroup group(String name) {
        return GROUPS.computeIfAbsent(name, SettingsGroup::new);
    }

    static boolean loaded() {
        return loaded;
    }

    static JsonObject data() {
        return FILE.data();
    }

    static java.nio.file.Path file() {
        return FILE.file();
    }

    static void ensureLoaded() {
        if (loaded) return;
        synchronized (HudLayout.class) {
            if (loaded) return;
            FILE.load();
            loaded = true;
            JsonObject data = FILE.data();
            if (data.size() == 0 && HudProfiles.active().main()) LegacyHudFile.migrate(data, FILE);
            HudRegistry.GLOBAL.set(HudStyle.defaults());
            HudRegistry.GLOBAL.overlay(HudStyle.fromJson(data.get("_global")));
            readCopies(data);
            for (Map.Entry<String, HudLayout.Placement> en : HudRegistry.PLACEMENTS.entrySet()) {
                PlacementJson.read(en.getKey(), en.getValue(), data.get(en.getKey()));
            }
            HudRegistry.rebuild();
        }
    }

    private static void readCopies(JsonObject data) {
        if (data.has("_copies") && data.get("_copies").isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : data.getAsJsonObject("_copies").entrySet()) {
                TreeSet<Integer> set = StateJson.readCopies(en.getValue());
                if (!set.isEmpty()) HudRegistry.COPIES.put(en.getKey(), set);
            }
        }
        for (HudElement e : new ArrayList<>(HudRegistry.ELEMENTS.values())) {
            TreeSet<Integer> c = HudRegistry.COPIES.get(e.id());
            if (c == null || !e.duplicable()) continue;
            for (int i : c) {
                if (HudRegistry.ELEMENTS.containsKey(e.id() + "." + i)) continue;
                HudElement copy = e.copy(i);
                HudRegistry.addCopy(copy, new HudLayout.Placement(copy));
            }
        }
    }

    static void saveIfLoaded() {
        if (loaded) save();
    }

    static void reload() {
        synchronized (HudLayout.class) {
            for (HudElement e : new ArrayList<>(HudRegistry.ELEMENTS.values())) {
                if (e.isCopy()) {
                    HudRegistry.ELEMENTS.remove(e.id());
                    HudRegistry.PLACEMENTS.remove(e.id());
                }
            }
            HudRegistry.COPIES.clear();
            FILE = group(HudProfiles.active().file());
            loaded = false;
            for (Map.Entry<String, HudLayout.Placement> en : HudRegistry.PLACEMENTS.entrySet()) {
                HudElement e = HudRegistry.ELEMENTS.get(en.getKey());
                if (e != null) Placements.reset(en.getValue(), e);
            }
            ensureLoaded();
        }
        HudCaches.styleChanged();
    }

    static void save() {
        ensureLoaded();
        JsonObject data = FILE.data();
        synchronized (FILE) {
            for (Map.Entry<String, HudLayout.Placement> en : HudRegistry.PLACEMENTS.entrySet()) {
                HudLayout.Placement p = en.getValue();
                if (!p.custom) data.remove(en.getKey());
                else data.add(en.getKey(), PlacementJson.write(p));
            }
            JsonObject g = HudRegistry.GLOBAL.toJson();
            JsonObject dj = HudStyle.defaults().toJson();
            for (String k : dj.keySet()) if (dj.get(k).equals(g.get(k))) g.remove(k);
            if (g.size() > 0) data.add("_global", g);
            else data.remove("_global");
            JsonObject c = StateJson.copiesJson(HudRegistry.COPIES);
            if (c.size() > 0) data.add("_copies", c);
            else data.remove("_copies");
            data.addProperty("_saved", true);
        }
        FILE.markDirty();
    }

    static boolean flag(String key) {
        ensureLoaded();
        JsonElement f = FILE.data().get("_flags");
        return f != null && f.isJsonObject() && f.getAsJsonObject().has(key) && f.getAsJsonObject().get(key).getAsBoolean();
    }

    static void setFlag(String key, boolean on) {
        ensureLoaded();
        JsonObject f = FILE.data().has("_flags") && FILE.data().get("_flags").isJsonObject()
                ? FILE.data().getAsJsonObject("_flags") : new JsonObject();
        if (on) f.addProperty(key, true);
        else f.remove(key);
        FILE.data().add("_flags", f);
        FILE.markDirty();
    }

    static boolean fresh() {
        ensureLoaded();
        return !FILE.data().has("_saved");
    }
}

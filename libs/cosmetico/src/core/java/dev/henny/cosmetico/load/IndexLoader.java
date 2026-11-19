package dev.henny.cosmetico.load;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.cosmetico.host.BundleResources;
import dev.henny.cosmetico.host.Log;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.parse.Bundles;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemRegistry;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.cosmetico.thumbs.ResourceReader;
import dev.henny.cosmetico.version.VersionGate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class IndexLoader {
    private final ItemRegistry items;
    private final VersionGate gate;
    private final BundleResources resources;
    private final Log log;

    public IndexLoader(ItemRegistry items, VersionGate gate, BundleResources resources, Log log) {
        this.items = items;
        this.gate = gate;
        this.resources = resources;
        this.log = log;
    }

    public Map<String, Item> read(ResourceReader reader, String root, ItemSource source) {
        Map<String, Item> out = new HashMap<>();
        JsonObject index = index(reader, root);
        if (index == null) return out;
        for (JsonElement el : index.getAsJsonArray("items")) {
            IndexEntry e = IndexEntry.of(el.getAsJsonObject());
            try {
                byte[] bytes = reader.read(root + e.file());
                if (bytes == null) continue;
                Bundle b = Bundles.parse(bytes, gate);
                resources.prepare(b);
                String thumb = e.thumbnail() == null ? null : root + e.thumbnail();
                out.put(b.id, Item.of(b, source, e.sort(), thumb, "default", items.nextRevision()));
            } catch (Exception ex) {
                log.error("Bundle " + e.file() + " in " + root + " is invalid: " + ex.getMessage(), null);
            }
        }
        log.info("Loaded " + out.size() + " cosmetics/emotes from " + root);
        return out;
    }

    private JsonObject index(ResourceReader reader, String root) {
        try {
            byte[] bytes = reader.read(root + "index.json");
            if (bytes == null) {
                log.warn("No cosmetics index at " + root);
                return null;
            }
            return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            log.error("Cosmetics index at " + root + " unreadable", e);
            return null;
        }
    }
}

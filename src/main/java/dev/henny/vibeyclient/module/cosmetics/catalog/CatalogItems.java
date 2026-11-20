package dev.henny.vibeyclient.module.cosmetics.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Kind;
import dev.henny.cosmetico.model.Rarity;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.model.Text;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemInfo;
import dev.henny.cosmetico.registry.ItemMeta;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.vibeyclient.BuildInfo;

import java.util.ArrayList;
import java.util.List;

final class CatalogItems {
    private CatalogItems() {}

    static Item item(CatalogEntry e, Bundle b) {
        String thumb = CatalogUrls.absolute(e.string("thumbnailUrl"));
        String rarity = e.string("rarity");
        ItemInfo info = new ItemInfo(b.id, b.kind, b.slot, text(e.json().get("name"), b.name), text(e.json().get("description"), b.description),
                b.author, b.tags, rarity == null ? b.rarity : Rarity.of(rarity));
        ItemMeta meta = new ItemMeta(e.sort(), thumb, e.hash(), e.availability(), creator(e.json()), null);
        return new Item(info, ItemSource.REMOTE, meta, b, Cosmetico.items().nextRevision());
    }

    static Item stub(CatalogEntry e, String required) {
        String kindS = e.string("kind");
        Kind kind = "emote".equals(kindS) ? Kind.EMOTE : "cosmetic".equals(kindS) ? Kind.COSMETIC : null;
        Slot slot = e.string("slot") == null ? null : Slot.of(e.string("slot"));
        if (kind == null || (kind == Kind.COSMETIC && slot == null)) {
            CatalogLog.once(e.id() + "@kind", "Catalog item {} has a kind/slot this client does not know ({}/{}), skipped", e.id(), kindS, e.json().get("slot"));
            return null;
        }
        CatalogLog.once(e.id() + "@" + e.hash() + "@update", "Catalog item {} needs Vibey Client {} (this is {})", e.id(),
                required == null ? "(newer format)" : required, BuildInfo.VERSION);
        String author = e.string("author");
        ItemInfo info = new ItemInfo(e.id(), kind, kind == Kind.EMOTE ? null : slot, text(e.json().get("name"), new Text(e.id(), null)),
                text(e.json().get("description"), Text.EMPTY), author == null ? "" : author, tags(e.json()), Rarity.of(e.string("rarity")));
        ItemMeta meta = new ItemMeta(e.sort(), CatalogUrls.absolute(e.string("thumbnailUrl")), e.hash(), e.availability(),
                creator(e.json()), required == null ? "999.0.0" : required);
        return new Item(info, ItemSource.REMOTE, meta, null, Cosmetico.items().nextRevision());
    }

    private static List<String> tags(JsonObject o) {
        List<String> tags = new ArrayList<>();
        if (o.get("tags") instanceof JsonArray ta) for (JsonElement t : ta) if (t.isJsonPrimitive()) tags.add(t.getAsString());
        return tags;
    }

    private static String creator(JsonObject o) {
        if (!(o.get("creator") instanceof JsonObject c)) return null;
        if (c.get("uuid") instanceof JsonPrimitive u && "00000000-0000-0000-0000-000000000000".equals(u.getAsString())) return Item.DELETED_CREATOR;
        return c.get("name") instanceof JsonPrimitive n ? n.getAsString() : null;
    }

    private static Text text(JsonElement e, Text fallback) {
        if (!(e instanceof JsonObject o)) return fallback;
        String en = o.has("en") && !o.get("en").isJsonNull() ? o.get("en").getAsString() : fallback.en();
        String de = o.has("de") && !o.get("de").isJsonNull() ? o.get("de").getAsString() : fallback.de();
        return new Text(en, de);
    }
}

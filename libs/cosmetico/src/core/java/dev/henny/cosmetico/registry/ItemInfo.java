package dev.henny.cosmetico.registry;

import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Kind;
import dev.henny.cosmetico.model.Rarity;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.model.Text;

import java.util.List;

public record ItemInfo(String id, Kind kind, Slot slot, Text name, Text description, String author, List<String> tags, Rarity rarity) {
    public ItemInfo {
        name = name == null ? Text.EMPTY : name;
        description = description == null ? Text.EMPTY : description;
        author = author == null ? "" : author;
        tags = tags == null ? List.of() : List.copyOf(tags);
        rarity = rarity == null ? Rarity.COMMON : rarity;
    }

    public static ItemInfo of(Bundle b) {
        return new ItemInfo(b.id, b.kind, b.slot, b.name, b.description, b.author, b.tags, b.rarity);
    }
}

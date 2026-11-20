package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.registry.Item;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import dev.henny.vibeyclient.module.cosmetics.ui.RarityColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ItemList {
    private ItemList() {}

    static List<Item> items(Slots.Def slot, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Item> out = new ArrayList<>();
        for (Item item : Cosmetico.items().all()) {
            if (!slot.holds(item)) continue;
            if (!q.isEmpty() && !matches(item, q)) continue;
            out.add(item);
        }
        return out;
    }

    static int count(Slots.Def slot) {
        int n = 0;
        for (Item item : Cosmetico.items().all()) if (slot.holds(item)) n++;
        return n;
    }

    static ItemRow row(Item item, Wardrobe wd, String selected) {
        boolean worn = worn(item, wd);
        boolean locked = wd != null && !wd.canUse(item);
        String rarity = rarity(item);
        String tip = item.name() + " · " + rarity + (item.creator() == null || item.creator().isEmpty() ? "" : " · " + item.creator());
        return new ItemRow(item, item.id(), item.name(), tip, Thumbs.of(item), color(item), worn, locked, item.id().equals(selected));
    }

    static boolean worn(Item item, Wardrobe wd) {
        if (wd == null) return false;
        if (item.isEmote()) {
            for (int i = 0; i < Wardrobe.WHEEL_SLOTS; i++) if (item.id().equals(wd.wheel(i))) return true;
            return false;
        }
        return item.id().equals(wd.equipped(item.slot()));
    }

    static String rarity(Item item) {
        return Lang.tr("ui.rarity." + item.rarity().id());
    }

    static String color(Item item) {
        return String.format("#%06x", RarityColors.of(item.rarity()) & 0xFFFFFF);
    }

    private static boolean matches(Item item, String q) {
        if (item.name().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (item.id().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (item.author() != null && item.author().toLowerCase(Locale.ROOT).contains(q)) return true;
        for (String t : item.tags()) if (t.toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }
}

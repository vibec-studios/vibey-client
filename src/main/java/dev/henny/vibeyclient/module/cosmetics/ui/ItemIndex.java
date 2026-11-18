package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The registry split into wardrobe categories and filtered by the search text. Rebuilt only when
 * {@link AvatarItems#version()}, the language (localised names) or the query change, so drawing never allocates lists.
 */
final class ItemIndex {
    private final List<AvatarItem>[] lists;
    private final String[] countText = new String[Category.ALL.length];
    private long version = -1;
    private String lang;
    private String query = "";
    private int total;
    private int matched;

    @SuppressWarnings("unchecked")
    ItemIndex() {
        lists = new List[Category.ALL.length];
        for (int i = 0; i < lists.length; i++) lists[i] = new ArrayList<>();
    }

    /** Call once per frame before reading. */
    void refresh(String q) {
        if (q == null) q = "";
        long v = AvatarItems.version();
        String l = Lang.code();
        if (v == version && l.equals(lang) && q.equals(query)) return;
        version = v;
        lang = l;
        query = q;
        rebuild();
    }

    private void rebuild() {
        for (List<AvatarItem> list : lists) list.clear();
        String q = query.trim().toLowerCase(Locale.ROOT);
        total = 0;
        matched = 0;
        for (AvatarItem item : AvatarItems.all()) {
            Category c = Category.of(item);
            if (c == null) continue;
            total++;
            if (!q.isEmpty() && !matches(item, q)) continue;
            lists[c.ordinal()].add(item);
            matched++;
        }
        for (int i = 0; i < lists.length; i++) countText[i] = Integer.toString(lists[i].size());
    }

    private static boolean matches(AvatarItem item, String q) {
        if (item.name().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (item.id().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (item.author().toLowerCase(Locale.ROOT).contains(q)) return true;
        for (String t : item.tags()) if (t.toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }

    List<AvatarItem> items(Category c) {
        return lists[c.ordinal()];
    }

    String countText(Category c) {
        return countText[c.ordinal()];
    }

    /** All cosmetics and emotes in the registry (ignores the search). */
    int total() {
        return total;
    }

    /** Items matching the search. */
    int matched() {
        return matched;
    }

    boolean searching() {
        return !query.isBlank();
    }

    String query() {
        return query;
    }
}

package dev.henny.vibeyclient.module.inventory.search;

import dev.henny.vibeyclient.module.inventory.core.SearchQuery;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public final class SearchState {
    private static final int REFRESH_FRAMES = 5;

    private static SearchQuery query = SearchQuery.NONE;
    private static boolean[] hits = new boolean[0];
    private static ItemStack[] seen = new ItemStack[0];
    private static int count;
    private static int frame;

    private SearchState() {}

    public static void query(String raw) {
        String text = raw == null ? "" : raw;
        if (text.equals(query.raw())) return;
        query = SearchQuery.parse(text);
        Arrays.fill(seen, null);
        frame = 0;
    }

    public static String raw() {
        return query.raw();
    }

    public static boolean active() {
        return !query.isEmpty();
    }

    public static int count() {
        return count;
    }

    public static boolean hit(int slot) {
        return slot >= 0 && slot < hits.length && hits[slot];
    }

    public static void clear() {
        query = SearchQuery.NONE;
        count = 0;
        Arrays.fill(hits, false);
    }

    public static void refresh(AbstractContainerMenu menu) {
        if (query.isEmpty()) {
            count = 0;
            return;
        }
        int n = menu.slots.size();
        if (hits.length != n) {
            hits = new boolean[n];
            seen = new ItemStack[n];
            frame = 0;
        }
        if (frame++ % REFRESH_FRAMES != 0) return;
        int c = 0;
        for (int i = 0; i < n; i++) {
            Slot s = menu.slots.get(i);
            ItemStack st = s.getItem();
            if (st != seen[i]) {
                seen[i] = st;
                hits[i] = !st.isEmpty() && query.matches(ItemFactsReader.read(st));
            }
            if (hits[i] && s.isActive()) c++;
        }
        count = c;
    }
}

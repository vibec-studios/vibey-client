package dev.henny.vibeyclient.module.chat.screen;

import dev.henny.vibeyclient.module.chat.core.SearchQuery;

public final class SearchState {
    private static volatile SearchQuery query = SearchQuery.NONE;
    private static volatile boolean open;

    private SearchState() {}

    public static SearchQuery query() {
        return query;
    }

    public static boolean open() {
        return open;
    }

    static void open(boolean on) {
        open = on;
        if (!on) query = SearchQuery.NONE;
    }

    static void query(String text) {
        query = SearchQuery.of(text);
    }
}

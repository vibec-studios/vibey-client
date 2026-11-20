package dev.henny.vibeyclient.module.chat.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vui.bind.State;

public final class SearchController {
    public final State<String> hits = new State<>("");
    public final State<Boolean> empty = new State<>(true);

    public String t(String key) {
        return Lang.tr(key);
    }

    void update(String query, int count) {
        empty.set(query.isBlank());
        hits.set(query.isBlank() ? Lang.tr("chat.search.hint") : Lang.tr("chat.search.hits", count));
    }

    public void close() {
        SearchMode.exit();
    }
}

package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vui.bind.State;

public final class BarController {
    public static final String SERVERS = "servers", FRIENDS = "friends";
    static final float SEARCH_WIDE = 108, SEARCH_NARROW = 84;

    private static String lastTab = SERVERS;

    public final State<String> query = new State<>("");
    public final State<Boolean> favOnly = new State<>(false);
    public final State<Boolean> searchOn = new State<>(true);
    public final State<Boolean> favOn = new State<>(true);
    public final State<Integer> favCount = new State<>(0);
    public final State<String> shown = new State<>("");
    public final State<Float> left = new State<>(0f);
    public final State<Float> right = new State<>(0f);
    public final State<Float> top = new State<>(0f);
    public final State<Float> searchWidth = new State<>(SEARCH_WIDE);
    public final State<Boolean> tabsOn = new State<>(false);
    public final State<String> tab = new State<>(lastTab);
    public final State<Integer> friendsCount = new State<>(0);
    public final State<Boolean> dropHot = new State<>(false);

    public String tr(String key) {
        return Lang.tr(key);
    }

    public void toggleFavOnly() {
        favOnly.set(!favOnly.peek());
    }

    public void clear() {
        query.set("");
    }

    public void showServers() {
        select(SERVERS);
    }

    public void showFriends() {
        select(FRIENDS);
    }

    public boolean friends() {
        return tabsOn.peek() && FRIENDS.equals(tab.peek());
    }

    void select(String t) {
        lastTab = t;
        if (!t.equals(tab.peek())) tab.set(t);
    }

    void place(float l, float r, float t) {
        if (left.peek() != l) left.set(l);
        if (right.peek() != r) right.set(r);
        if (top.peek() != t) top.set(t);
    }

    void tabs(boolean on, int playing) {
        if (tabsOn.peek() != on) tabsOn.set(on);
        float w = on ? SEARCH_NARROW : SEARCH_WIDE;
        if (searchWidth.peek() != w) searchWidth.set(w);
        if (friendsCount.peek() != playing) friendsCount.set(playing);
    }

    void hot(boolean on) {
        if (dropHot.peek() != on) dropHot.set(on);
    }

    void show(boolean search, boolean favs, int favourites, int visible) {
        if (searchOn.peek() != search) searchOn.set(search);
        if (favOn.peek() != favs) favOn.set(favs);
        if (favCount.peek() != favourites) favCount.set(favourites);
        String s = query.peek().isBlank() && !favOnly.peek() ? "" : Integer.toString(visible);
        if (!s.equals(shown.peek())) shown.set(s);
    }
}

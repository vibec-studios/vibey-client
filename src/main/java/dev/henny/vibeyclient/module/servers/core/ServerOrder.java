package dev.henny.vibeyclient.module.servers.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ServerOrder {
    private ServerOrder() {}

    public static <T> List<T> arrange(List<T> entries, Predicate<T> keep, Predicate<T> favourite, boolean favouritesFirst) {
        List<T> top = new ArrayList<>();
        List<T> rest = new ArrayList<>();
        for (T e : entries) {
            if (!keep.test(e)) continue;
            if (favouritesFirst && favourite.test(e)) top.add(e);
            else rest.add(e);
        }
        top.addAll(rest);
        return top;
    }

    public static <T> boolean reordered(List<T> original, List<T> shown) {
        if (original.size() != shown.size()) return true;
        for (int i = 0; i < original.size(); i++) if (original.get(i) != shown.get(i)) return true;
        return false;
    }
}

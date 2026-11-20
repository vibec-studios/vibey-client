package dev.henny.vibeyclient.module.clips.shots.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ShotList {
    private ShotList() {}

    public static List<Shot> view(Collection<Shot> all, Set<String> favourites, ShotSort sort, boolean favouritesOnly) {
        List<Shot> out = new ArrayList<>(all.size());
        for (Shot s : all) if (!favouritesOnly || favourites.contains(s.name())) out.add(s);
        out.sort(sort.comparator());
        return out;
    }

    public static int indexOf(List<Shot> list, String name) {
        if (name == null) return -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).name().equals(name)) return i;
        return -1;
    }

    public static Shot step(List<Shot> list, String current, int delta) {
        if (list.isEmpty()) return null;
        int i = indexOf(list, current);
        if (i < 0) return list.get(0);
        int n = list.size();
        return list.get(((i + delta) % n + n) % n);
    }

    public static Shot keepOrNear(List<Shot> list, String current, int previousIndex) {
        if (list.isEmpty()) return null;
        int i = indexOf(list, current);
        if (i >= 0) return list.get(i);
        return list.get(Math.max(0, Math.min(list.size() - 1, previousIndex)));
    }

    public static List<Shot> window(List<Shot> list, String current, int size) {
        if (list.size() <= size) return List.copyOf(list);
        int i = Math.max(0, indexOf(list, current));
        int start = Math.max(0, Math.min(list.size() - size, i - size / 2));
        return List.copyOf(list.subList(start, start + size));
    }
}

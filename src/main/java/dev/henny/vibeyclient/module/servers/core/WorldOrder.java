package dev.henny.vibeyclient.module.servers.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WorldOrder {
    public record World(String id, String name, long lastPlayed) {}

    private static final Comparator<World> RECENT = Comparator.comparingLong(World::lastPlayed).reversed()
            .thenComparing(World::id);
    private static final Comparator<World> BY_NAME = Comparator.comparing((World w) -> w.name().toLowerCase(Locale.ROOT))
            .thenComparing(World::id);

    private WorldOrder() {}

    public static List<String> arrange(List<World> worlds, List<String> custom, WorldSort sort) {
        List<World> sorted = new ArrayList<>(worlds);
        sorted.sort(sort == WorldSort.NAME ? BY_NAME : RECENT);
        if (sort != WorldSort.CUSTOM) return ids(sorted);
        Map<String, World> byId = new LinkedHashMap<>();
        for (World w : sorted) byId.put(w.id(), w);
        Set<String> known = new HashSet<>(custom);
        List<String> out = new ArrayList<>();
        for (World w : sorted) if (!known.contains(w.id())) out.add(w.id());
        for (String id : custom) if (byId.containsKey(id) && !out.contains(id)) out.add(id);
        return out;
    }

    public static List<String> drop(List<String> full, List<String> shown, int from, int slot) {
        List<String> out = new ArrayList<>(full);
        if (DropSlot.noop(from, slot) || from < 0 || from >= shown.size()) return out;
        String moved = shown.get(from);
        out.remove(moved);
        if (slot < shown.size()) out.add(out.indexOf(shown.get(slot)), moved);
        else out.add(out.indexOf(shown.get(shown.size() - 1)) + 1, moved);
        return out;
    }

    private static List<String> ids(List<World> worlds) {
        List<String> out = new ArrayList<>(worlds.size());
        for (World w : worlds) out.add(w.id());
        return out;
    }
}

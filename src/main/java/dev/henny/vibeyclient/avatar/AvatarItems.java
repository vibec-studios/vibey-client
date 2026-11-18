package dev.henny.vibeyclient.avatar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of every cosmetic and emote, merged by id from three sources (DEV over CATALOG over BUILTIN). Reads are
 * lock free snapshots and safe from any thread; writes happen on the client thread (loaders hop there). UIs poll
 * {@link #version()} to know when to refresh.
 */
public final class AvatarItems {
    private AvatarItems() {}

    private static final Map<ItemSource, Map<String, AvatarItem>> SOURCES = new EnumMap<>(ItemSource.class);
    private static volatile Snapshot snapshot = new Snapshot(Map.of(), List.of());
    private static volatile long version;
    private static long revisions;
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private record Snapshot(Map<String, AvatarItem> byId, List<AvatarItem> sorted) {}

    static {
        for (ItemSource s : ItemSource.values()) SOURCES.put(s, new HashMap<>());
    }

    private static final Comparator<AvatarItem> ORDER = Comparator
            .comparingInt((AvatarItem i) -> i.isEmote() ? 100 : i.slot() == null ? 99 : i.slot().ordinal())
            .thenComparingInt(AvatarItem::sort)
            .thenComparing(AvatarItem::id);

    // ============================================================================================ reads

    /** All items: cosmetics by slot then sort order, then emotes. */
    public static List<AvatarItem> all() {
        return snapshot.sorted;
    }

    /** The winning item for {@code id}, or null. */
    public static AvatarItem get(String id) {
        return id == null ? null : snapshot.byId.get(id);
    }

    public static List<AvatarItem> bySlot(Slot slot) {
        List<AvatarItem> out = new ArrayList<>();
        for (AvatarItem i : snapshot.sorted) if (!i.isEmote() && i.slot() == slot) out.add(i);
        return out;
    }

    public static List<AvatarItem> emotes() {
        List<AvatarItem> out = new ArrayList<>();
        for (AvatarItem i : snapshot.sorted) if (i.isEmote()) out.add(i);
        return out;
    }

    /** The item from one specific source (e.g. the builtin version under a dev item), or null. */
    public static synchronized AvatarItem get(String id, ItemSource source) {
        return SOURCES.get(source).get(id);
    }

    /** Increases on every change. */
    public static long version() {
        return version;
    }

    /** Called (client thread) after every change. */
    public static void addListener(Runnable r) {
        LISTENERS.add(r);
    }

    // ============================================================================================ writes (engine only)

    public static synchronized long nextRevision() {
        return ++revisions;
    }

    public static synchronized void put(AvatarItem item) {
        SOURCES.get(item.source()).put(item.id(), item);
        rebuild();
    }

    public static synchronized void putAll(ItemSource source, Map<String, AvatarItem> items, boolean replace) {
        Map<String, AvatarItem> m = SOURCES.get(source);
        if (replace) m.clear();
        m.putAll(items);
        rebuild();
    }

    public static synchronized AvatarItem remove(String id, ItemSource source) {
        AvatarItem old = SOURCES.get(source).remove(id);
        if (old != null) rebuild();
        return old;
    }

    private static void rebuild() {
        Map<String, AvatarItem> byId = new HashMap<>();
        for (ItemSource s : ItemSource.values()) byId.putAll(SOURCES.get(s)); // later sources win
        List<AvatarItem> sorted = new ArrayList<>(byId.values());
        sorted.sort(ORDER);
        snapshot = new Snapshot(Collections.unmodifiableMap(byId), Collections.unmodifiableList(sorted));
        version++;
        for (Runnable r : LISTENERS) {
            try {
                r.run();
            } catch (RuntimeException e) {
                dev.henny.vibeyclient.Vibey.LOG.error("AvatarItems listener failed", e);
            }
        }
    }
}

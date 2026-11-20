package dev.henny.vui.icon;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.Suggest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class IconRegistry {
    public static final String FILE = "icons.json";

    private final Map<String, IconSet> sets = new LinkedHashMap<>();
    private final Map<String, Diagnostics> problems = new LinkedHashMap<>();
    private Function<String, String> resources = path -> null;
    private int version = 1;

    public static String resourcePath(String namespace) {
        return "assets/" + namespace + "/vui/" + FILE;
    }

    public IconRegistry resources(Function<String, String> reader) {
        resources = reader == null ? path -> null : reader;
        return this;
    }

    public synchronized IconRegistry put(IconSet set) {
        sets.put(set.namespace(), set);
        version++;
        return this;
    }

    public synchronized IconSet load(String namespace, String fileName, String source) {
        Diagnostics d = new Diagnostics();
        IconSet set = IconSetParser.parse(namespace, fileName, source, d);
        problems.put(namespace, d);
        put(set);
        return set;
    }

    public synchronized boolean reload(String namespace) {
        sets.remove(namespace);
        problems.remove(namespace);
        version++;
        return set(namespace) != null;
    }

    public synchronized IconSet set(String namespace) {
        IconSet known = sets.get(namespace);
        if (known != null || problems.containsKey(namespace)) return known;
        String path = resourcePath(namespace);
        String source = resources.apply(path);
        if (source == null) {
            problems.put(namespace, new Diagnostics());
            return null;
        }
        return load(namespace, path, source);
    }

    public synchronized PixelIcon find(String ref, String defaultNamespace) {
        if (ref == null || ref.isEmpty()) return null;
        int colon = ref.indexOf(':');
        if (colon > 0) {
            IconSet s = set(ref.substring(0, colon));
            return s == null ? null : s.get(ref.substring(colon + 1));
        }
        if (defaultNamespace != null) {
            IconSet s = set(defaultNamespace);
            if (s != null && s.get(ref) != null) return s.get(ref);
        }
        for (IconSet s : sets.values()) if (s.get(ref) != null) return s.get(ref);
        return null;
    }

    public PixelIcon find(String ref) {
        return find(ref, null);
    }

    public synchronized String suggest(String ref) {
        List<String> all = new ArrayList<>();
        for (IconSet s : sets.values()) for (String n : s.names()) all.add(s.namespace() + ":" + n);
        String best = Suggest.closest(ref, all, 3);
        if (best == null && ref.indexOf(':') < 0) {
            for (IconSet s : sets.values()) {
                String local = Suggest.closest(ref, s.names(), 2);
                if (local != null) return s.namespace() + ":" + local;
            }
        }
        return best;
    }

    public synchronized List<IconSet> sets() {
        return new ArrayList<>(sets.values());
    }

    public synchronized Diagnostics diagnostics() {
        Diagnostics all = new Diagnostics();
        for (Diagnostics d : problems.values()) all.addAll(d);
        return all;
    }

    public synchronized int version() {
        return version;
    }
}

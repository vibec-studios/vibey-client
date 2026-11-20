package dev.henny.vui.reload;

import dev.henny.vui.Vui;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.markup.ComponentFile;
import dev.henny.vui.markup.ComponentLoader;
import dev.henny.vui.template.CompiledTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DocumentRegistry {
    private final Vui vui;
    private final List<ResourceRoot> overrides = new ArrayList<>();
    private final List<ResourceRoot> roots = new ArrayList<>();
    private final Map<String, CompiledTemplate> cache = new HashMap<>();
    private final Map<String, String> sources = new HashMap<>();

    public DocumentRegistry(Vui vui) {
        this.vui = vui;
    }

    public DocumentRegistry addRoot(ResourceRoot root) {
        roots.add(root);
        return this;
    }

    public DocumentRegistry addOverride(ResourceRoot root) {
        overrides.add(root);
        return this;
    }

    public List<ResourceRoot> allRoots() {
        List<ResourceRoot> all = new ArrayList<>(overrides);
        all.addAll(roots);
        return all;
    }

    public synchronized CompiledTemplate template(String id) {
        CompiledTemplate cached = cache.get(id);
        if (cached != null) return cached;
        CompiledTemplate loaded = load(id);
        cache.put(id, loaded);
        return loaded;
    }

    public synchronized boolean isLoaded(String id) {
        return cache.containsKey(id);
    }

    public synchronized List<String> loadedIds() {
        return new ArrayList<>(cache.keySet());
    }

    public synchronized String sourceOf(String id) {
        return sources.get(id);
    }

    public synchronized CompiledTemplate reload(String id) {
        cache.remove(id);
        return template(id);
    }

    public synchronized void forget(String id) {
        cache.remove(id);
    }

    private CompiledTemplate load(String id) {
        if (!DocumentId.isValid(id)) return missing(id, "'" + id + "' is not a document id", "use ns:path, e.g. mymod:settings");
        DocumentId doc = DocumentId.parse(id);
        for (ResourceRoot root : allRoots()) {
            for (String ext : new String[]{DocumentId.MARKUP, DocumentId.JSON}) {
                String rel = doc.resource(ext);
                String text = root.read(rel);
                if (text == null) continue;
                String file = root.describe(rel);
                sources.put(id, file);
                ComponentFile parsed = ComponentLoader.load(id, file, text);
                return vui.compile(parsed);
            }
        }
        return missing(id, "document '" + id + "' not found", "expected " + doc.resource(DocumentId.MARKUP) + " in a resource root");
    }

    private CompiledTemplate missing(String id, String message, String hint) {
        Diagnostics d = new Diagnostics();
        String file = "<" + id + ">";
        d.error(new SourcePos(file, 1, 1), message, hint);
        ComponentFile empty = new ComponentFile(id, file, List.of(), "", new SourcePos(file, 1, 1), Map.of(), Map.of(), d);
        return vui.compile(empty);
    }
}

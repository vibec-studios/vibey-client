package dev.henny.vui.reload;

import dev.henny.vui.dom.Document;
import dev.henny.vui.template.CompiledTemplate;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class HotReloader implements AutoCloseable {
    private final DocumentRegistry registry;
    private final List<WeakReference<Document>> documents = new ArrayList<>();
    private SourceWatcher watcher;
    private int reloads;

    public HotReloader(DocumentRegistry registry) {
        this.registry = registry;
    }

    public void track(Document doc) {
        documents.removeIf(r -> r.get() == null || r.get() == doc);
        documents.add(new WeakReference<>(doc));
    }

    public void untrack(Document doc) {
        documents.removeIf(r -> r.get() == null || r.get() == doc);
    }

    public List<Document> documents() {
        List<Document> out = new ArrayList<>();
        for (WeakReference<Document> r : documents) {
            Document d = r.get();
            if (d != null) out.add(d);
        }
        return out;
    }

    public boolean watching() {
        return watcher != null;
    }

    public void watch() throws IOException {
        if (watcher != null) return;
        List<Path> dirs = new ArrayList<>();
        for (ResourceRoot root : registry.allRoots()) if (root.directory() != null) dirs.add(root.directory());
        watcher = new SourceWatcher(dirs);
    }

    public int reloads() {
        return reloads;
    }

    public Set<String> poll() {
        if (watcher == null) return Set.of();
        Set<String> ids = new LinkedHashSet<>();
        for (Path p : watcher.drain()) {
            DocumentId id = idOf(p);
            if (id != null) ids.add(id.toString());
        }
        if (!ids.isEmpty()) apply(ids);
        return ids;
    }

    public void reloadAll() {
        apply(new LinkedHashSet<>(registry.loadedIds()));
    }

    public void apply(Set<String> changed) {
        for (String id : changed) registry.forget(id);
        reloads++;
        for (Document doc : documents()) {
            CompiledTemplate next = registry.template(doc.documentId());
            if (next.hasErrors() && !doc.template().hasErrors()) doc.reloadFailed(next.diagnostics());
            else doc.swap(next);
        }
    }

    private DocumentId idOf(Path p) {
        for (ResourceRoot root : registry.allRoots()) {
            if (root.directory() == null) continue;
            Path dir = root.directory();
            if (!p.startsWith(dir)) continue;
            String rel = dir.relativize(p).toString().replace('\\', '/');
            DocumentId id = DocumentId.fromResource(rel);
            if (id != null) return id;
        }
        return null;
    }

    @Override
    public void close() {
        if (watcher != null) watcher.close();
        watcher = null;
    }
}

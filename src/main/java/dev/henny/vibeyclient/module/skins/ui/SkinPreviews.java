package dev.henny.vibeyclient.module.skins.ui;

import dev.henny.vibeyclient.module.skins.SkinImages;
import dev.henny.vibeyclient.module.skins.core.BridgeFiles;
import dev.henny.vibeyclient.module.skins.core.LibrarySkin;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class SkinPreviews {
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vibey skin previews");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private final Map<String, Identifier> ready = new HashMap<>();
    private final Set<String> loading = new HashSet<>();
    private final Map<String, byte[]> loaded = new HashMap<>();
    private volatile boolean closed;

    Identifier get(Path vibeDir, LibrarySkin s) {
        String key = s.id() + "|" + s.file() + "|" + s.sha1();
        if (ready.containsKey(key)) return ready.get(key);
        byte[] png;
        synchronized (loaded) {
            png = loaded.remove(key);
        }
        if (png != null) {
            Identifier id = SkinImages.upload("preview", png);
            ready.put(key, id);
            return id;
        }
        if (vibeDir != null && loading.add(key)) {
            LOADER.execute(() -> {
                if (closed) return;
                byte[] b = BridgeFiles.readPng(vibeDir, s);
                synchronized (loaded) {
                    if (closed) return;
                    if (b != null) loaded.put(key, b);
                }
                if (b == null) loading.remove(key);
            });
        }
        return null;
    }

    void close() {
        closed = true;
        for (Identifier id : ready.values()) SkinImages.release(id);
        ready.clear();
        synchronized (loaded) {
            loaded.clear();
        }
        loading.clear();
    }
}

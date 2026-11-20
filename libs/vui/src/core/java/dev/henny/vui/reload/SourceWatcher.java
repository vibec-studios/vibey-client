package dev.henny.vui.reload;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public final class SourceWatcher implements AutoCloseable {
    private final WatchService service;
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private final ConcurrentLinkedQueue<Path> changed = new ConcurrentLinkedQueue<>();
    private final Thread thread;
    private volatile boolean running = true;

    public SourceWatcher(Iterable<Path> roots) throws IOException {
        this.service = FileSystems.getDefault().newWatchService();
        for (Path root : roots) if (Files.isDirectory(root)) registerTree(root);
        this.thread = new Thread(this::loop, "vui-source-watcher");
        thread.setDaemon(true);
        thread.start();
    }

    public Set<Path> drain() {
        Set<Path> out = new LinkedHashSet<>();
        Path p;
        while ((p = changed.poll()) != null) out.add(p);
        return out;
    }

    public void touch(Path path) {
        changed.add(path);
    }

    private void registerTree(Path root) throws IOException {
        try (Stream<Path> dirs = Files.walk(root)) {
            for (Path dir : (Iterable<Path>) dirs.filter(Files::isDirectory)::iterator) register(dir);
        }
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        synchronized (keys) {
            keys.put(key, dir);
        }
    }

    private void loop() {
        while (running) {
            WatchKey key;
            try {
                key = service.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                return;
            }
            Path dir;
            synchronized (keys) {
                dir = keys.get(key);
            }
            if (dir != null) handle(key, dir);
            if (!key.reset()) {
                synchronized (keys) {
                    keys.remove(key);
                }
            }
        }
    }

    private void handle(WatchKey key, Path dir) {
        for (WatchEvent<?> event : key.pollEvents()) {
            if (!(event.context() instanceof Path name)) continue;
            Path full = dir.resolve(name);
            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(full)) {
                try {
                    registerTree(full);
                } catch (IOException ignored) {
                }
                continue;
            }
            changed.add(full.toAbsolutePath().normalize());
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            service.close();
        } catch (IOException ignored) {
        }
        thread.interrupt();
    }
}

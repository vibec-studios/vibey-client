package dev.henny.companio.mc.packs;

import dev.henny.companio.mc.CompanioMod;
import dev.henny.companio.pack.Pack;
import dev.henny.companio.pack.PackFiles;
import dev.henny.companio.pack.PackRegistry;
import dev.henny.companio.parse.PackReader;
import dev.henny.cosmetico.Cosmetico;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PackRepository {
    private static final int WATCH_TICKS = 20;

    private final PackRegistry registry;
    private final UserPacks user;
    private final Supplier<List<Path>> builtinRoots;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "companio-packs");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean loading = new AtomicBoolean();
    private volatile List<Pack> pending;
    private List<PackFiles> builtins;
    private long stamp;
    private int ticks;
    private boolean reloadQueued;
    private Consumer<List<Pack>> onLoaded = p -> {};

    public PackRepository(PackRegistry registry, Path folder, Supplier<List<Path>> builtinRoots) {
        this.registry = registry;
        this.user = new UserPacks(folder);
        this.builtinRoots = builtinRoots;
    }

    public void onLoaded(Consumer<List<Pack>> listener) {
        onLoaded = listener;
    }

    public Path folder() {
        return user.dir();
    }

    public boolean loading() {
        return loading.get();
    }

    public void reload() {
        if (!loading.compareAndSet(false, true)) {
            reloadQueued = true;
            return;
        }
        worker.execute(() -> {
            try {
                pending = loadAll();
            } catch (RuntimeException e) {
                CompanioMod.LOG.error("Loading companion packs failed", e);
            } finally {
                loading.set(false);
            }
        });
    }

    private List<Pack> loadAll() {
        long s = user.stamp();
        if (builtins == null) builtins = BuiltinPacks.read(builtinRoots.get());
        List<Pack> out = new ArrayList<>();
        for (PackFiles f : builtins) out.add(PackReader.read(f, Cosmetico.versionGate(), true));
        for (Path p : user.entries()) out.add(PackReader.read(UserPacks.open(p), Cosmetico.versionGate(), false));
        stamp = s;
        return out;
    }

    public void tick() {
        List<Pack> done = pending;
        if (done != null) {
            pending = null;
            registry.replace(done);
            onLoaded.accept(done);
            if (reloadQueued) {
                reloadQueued = false;
                reload();
            }
        }
        if (++ticks % WATCH_TICKS != 0 || loading.get()) return;
        worker.execute(() -> {
            if (!loading.get() && user.stamp() != stamp) reload();
        });
    }
}

package dev.henny.vibeyclient.module.cosmetics.catalog;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.backend.net.Net;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class Catalog {
    private Catalog() {}

    private static final long POLL_MS = 10 * 60_000, POLL_JITTER_MS = 3 * 60_000, MIN_GAP_MS = 15_000;
    private static final long PUSH_POLL_MS = 60 * 60_000, PUSH_POLL_JITTER_MS = 15 * 60_000;

    public static void onNewItems(Consumer<Set<String>> listener) {
        CatalogState.onNewItems = listener;
    }

    public static Consumer<Set<String>> newItemsListener() {
        return CatalogState.onNewItems;
    }

    public static String catalogVersion() {
        return CatalogState.catalogVersion;
    }

    public static boolean catalogPushed() {
        return Services.get(VibeBackend.class).map(VibeBackend::isReady).orElse(false);
    }

    public static boolean catalogLoaded() {
        return CatalogState.loaded;
    }

    public static boolean catalogConfirmed() {
        return CatalogState.confirmed;
    }

    public static void refreshCatalog(boolean force) {
        if (!GlobalSettings.online()) return;
        long now = System.currentTimeMillis();
        if (force) CatalogState.nextCatalog = Math.min(CatalogState.nextCatalog, Math.max(now, CatalogState.lastCatalog + MIN_GAP_MS));
        if (now < CatalogState.nextCatalog || !CatalogState.RUNNING.compareAndSet(false, true)) return;
        CatalogState.lastCatalog = now;
        boolean pushed = catalogPushed();
        CatalogState.nextCatalog = now + (pushed ? PUSH_POLL_MS : POLL_MS) + ThreadLocalRandom.current().nextLong(pushed ? PUSH_POLL_JITTER_MS : POLL_JITTER_MS);
        int gen = CatalogState.GENERATION.get();
        String url = CatalogUrls.base() + CatalogUrls.CATALOG_PATH;
        CompletableFuture.supplyAsync(() -> CatalogLoad.load(gen, url), CatalogIo.EXECUTOR).thenCompose(f -> f).whenComplete((v, e) -> finished(gen, e));
    }

    private static void finished(int gen, Throwable e) {
        if (e != null) {
            Vibey.LOG.info("Cosmetics catalog not available: {}", Net.cause(e).toString());
            CatalogState.nextCatalog = Math.min(CatalogState.nextCatalog, System.currentTimeMillis() + 60_000 + ThreadLocalRandom.current().nextLong(30_000));
        }
        CatalogState.RUNNING.set(false);
        Tasks.onMain(() -> {
            if (CatalogState.loaded && gen == CatalogState.GENERATION.get()) CatalogState.confirmed = true;
            checkReadyVersion();
        });
    }

    public static void requestCatalog(long jitterMs) {
        long at = System.currentTimeMillis() + (jitterMs > 0 ? ThreadLocalRandom.current().nextLong(jitterMs) : 0);
        CatalogState.nextCatalog = Math.min(CatalogState.nextCatalog, Math.max(at, CatalogState.lastCatalog + MIN_GAP_MS));
    }

    public static void readyVersion(String v) {
        if (CatalogState.RUNNING.get()) {
            CatalogState.pendingReadyVersion = v == null ? "" : v;
            return;
        }
        if (v == null || !v.equals(CatalogState.catalogVersion)) requestCatalog(10_000);
    }

    private static void checkReadyVersion() {
        String v = CatalogState.pendingReadyVersion;
        if (v == null) return;
        CatalogState.pendingReadyVersion = null;
        if (v.isEmpty() || !v.equals(CatalogState.catalogVersion)) requestCatalog(10_000);
    }

    public static void dropCatalog() {
        CatalogState.GENERATION.incrementAndGet();
        CatalogState.nextCatalog = 0;
        CatalogState.loaded = false;
        CatalogState.confirmed = false;
        CatalogState.lastIds = null;
        CatalogState.applied = null;
        CatalogState.parsed = Map.of();
        Cosmetico.items().putAll(ItemSource.REMOTE, Map.of(), true);
    }
}

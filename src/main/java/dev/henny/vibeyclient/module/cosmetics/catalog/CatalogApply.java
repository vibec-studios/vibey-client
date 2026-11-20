package dev.henny.vibeyclient.module.cosmetics.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.parse.NeedsUpdateException;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.backend.net.Net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class CatalogApply {
    private final boolean download;
    private final int gen;
    private final boolean fromDisk;
    private final Map<String, Item> items = new HashMap<>();
    private final Map<String, Bundle> bundles = new ConcurrentHashMap<>();
    private final List<CatalogEntry> missing = new ArrayList<>();
    private boolean skipped;

    private CatalogApply(boolean download, int gen, boolean fromDisk) {
        this.download = download;
        this.gen = gen;
        this.fromDisk = fromDisk;
    }

    static CompletableFuture<Boolean> apply(JsonObject catalog, boolean download, int gen, boolean fromDisk) {
        return new CatalogApply(download, gen, fromDisk).run(catalog);
    }

    private CompletableFuture<Boolean> run(JsonObject catalog) {
        JsonArray list = catalog.get("items") instanceof JsonArray a ? a : new JsonArray();
        String version = catalog.get("version") instanceof JsonPrimitive v ? v.getAsString() : null;
        Map<String, Bundle> before = CatalogState.parsed;
        for (JsonElement el : list) known(entry(el), before);
        List<CompletableFuture<Void>> jobs = new ArrayList<>();
        for (CatalogEntry e : missing) jobs.add(download(e));
        boolean complete = !skipped;
        return CompletableFuture.allOf(jobs.toArray(new CompletableFuture[0])).thenApply(x -> {
            if (!missing.isEmpty()) Vibey.LOG.info("Cosmetics catalog: downloaded {} new/changed asset(s)", missing.size());
            Vibey.LOG.info("Cosmetics catalog: {} items", items.size());
            if (gen == CatalogState.GENERATION.get()) CatalogState.parsed = Map.copyOf(bundles);
            CatalogSwap.swap(items, version, gen, fromDisk);
            return complete;
        });
    }

    private static CatalogEntry entry(JsonElement el) {
        try {
            return CatalogEntry.of(el);
        } catch (RuntimeException e) {
            CatalogLog.once("entry:" + el.hashCode(), "Catalog entry skipped: {}", e.toString());
            return null;
        }
    }

    private void known(CatalogEntry e, Map<String, Bundle> before) {
        if (e == null) return;
        try {
            if (e.minClient() != null && !Cosmetico.versionGate().satisfies(e.minClient())) {
                putStub(e, e.minClient());
                return;
            }
            Item old = Cosmetico.items().get(e.id(), ItemSource.REMOTE);
            Bundle b = old != null && old.bundle() != null && e.hash().equals(old.hash()) ? old.bundle() : null;
            if (b == null) b = before.get(e.hash());
            if (b == null) b = AssetCache.fromDisk(e);
            if (b == null) {
                notOnDisk(e, old);
                return;
            }
            bundles.put(e.hash(), b);
            items.put(e.id(), CatalogItems.item(e, b));
        } catch (NeedsUpdateException nu) {
            putStub(e, nu.required);
        } catch (Exception ex) {
            if (CatalogState.FAILED.add(e.hash())) Vibey.LOG.warn("Catalog item {} skipped: {}", e.id(), ex.getMessage());
        }
    }

    private void notOnDisk(CatalogEntry e, Item old) {
        if (download && !CatalogState.FAILED.contains(e.hash())) {
            missing.add(e);
            return;
        }
        skipped = true;
        if (old != null && old.bundle() != null) items.put(e.id(), old);
    }

    private void putStub(CatalogEntry e, String required) {
        Item stub = CatalogItems.stub(e, required);
        if (stub != null) synchronized (items) {
            items.put(e.id(), stub);
        }
    }

    private CompletableFuture<Void> download(CatalogEntry e) {
        return CatalogIo.DOWNLOADS.submit(() -> CatalogIo.ASSET_FLIGHT.run(e.hash(), () -> CatalogIo.fetch(CatalogUrls.absolute(e.url()), Net.Kind.BULK)))
                .thenApplyAsync(bytes -> AssetCache.verifyAndParse(e, bytes), CatalogIo.EXECUTOR)
                .handle((b, ex) -> {
                    downloaded(e, b, ex);
                    return null;
                });
    }

    private void downloaded(CatalogEntry e, Bundle b, Throwable ex) {
        if (ex == null) {
            bundles.put(e.hash(), b);
            synchronized (items) {
                items.put(e.id(), CatalogItems.item(e, b));
            }
        } else if (Net.cause(ex) instanceof NeedsUpdateException nu) {
            putStub(e, nu.required);
        } else if (CatalogState.FAILED.add(e.hash())) {
            Vibey.LOG.warn("Catalog item {} skipped: {}", e.id(), Net.cause(ex).toString());
        }
        Item old = Cosmetico.items().get(e.id(), ItemSource.REMOTE);
        synchronized (items) {
            if (!items.containsKey(e.id()) && old != null && old.bundle() != null) items.put(e.id(), old);
        }
    }
}

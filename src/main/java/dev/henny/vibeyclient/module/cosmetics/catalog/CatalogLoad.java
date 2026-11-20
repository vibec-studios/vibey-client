package dev.henny.vibeyclient.module.cosmetics.catalog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.backend.net.HttpCache;
import dev.henny.vibeyclient.backend.net.Net;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

final class CatalogLoad {
    private CatalogLoad() {}

    static CompletableFuture<Void> load(int gen, String url) {
        try {
            Files.createDirectories(CatalogIo.cacheDir().resolve("assets"));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
        HttpCache cache = new HttpCache(CatalogIo.cacheDir());
        HttpCache.Entry cached = cache.load("catalog");
        String base = url.substring(0, url.length() - CatalogUrls.CATALOG_PATH.length());
        CompletableFuture<HttpCache.Result> net = cache.fetch("catalog", url, cached, Net.Kind.API, CatalogIo.EXECUTOR).thenApply(r -> {
            AssetCache.writeBase(base);
            return r;
        });
        CompletableFuture<Void> shown = showDiskCopy(gen, cached, base);
        return shown.handle((x, e) -> null).thenCompose(x -> net).thenComposeAsync(res -> applyNetwork(gen, res), CatalogIo.EXECUTOR);
    }

    private static CompletableFuture<Void> showDiskCopy(int gen, HttpCache.Entry cached, String base) {
        CatalogState.Applied a = CatalogState.applied;
        if (cached == null || (a != null && a.gen() == gen) || !base.equals(AssetCache.readBase())) return CompletableFuture.completedFuture(null);
        try {
            JsonObject cat = JsonParser.parseString(new String(cached.body(), StandardCharsets.UTF_8)).getAsJsonObject();
            return CatalogApply.apply(cat, false, gen, true).thenAccept(complete -> CatalogState.applied = new CatalogState.Applied(gen, cached.etag(), complete));
        } catch (RuntimeException e) {
            Vibey.LOG.debug("Cached catalog unreadable: {}", e.toString());
            return CompletableFuture.completedFuture(null);
        }
    }

    private static CompletableFuture<Void> applyNetwork(int gen, HttpCache.Result res) {
        CatalogState.Applied now = CatalogState.applied;
        if (res.notModified() && now != null && now.gen() == gen && now.complete() && Objects.equals(now.etag(), res.etag())) {
            return CompletableFuture.completedFuture(null);
        }
        JsonObject cat = JsonParser.parseString(new String(res.body(), StandardCharsets.UTF_8)).getAsJsonObject();
        return CatalogApply.apply(cat, true, gen, false).thenAccept(complete -> CatalogState.applied = new CatalogState.Applied(gen, res.etag(), complete));
    }
}

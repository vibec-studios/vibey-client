package dev.henny.vibeyclient.module.cosmetics.catalog;

import dev.henny.cosmetico.host.ThumbnailFetcher;
import dev.henny.cosmetico.parse.Bundles;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.backend.net.Net;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class CatalogThumbnails implements ThumbnailFetcher {
    public static final CatalogThumbnails INSTANCE = new CatalogThumbnails();

    private CatalogThumbnails() {}

    @Override
    public CompletableFuture<byte[]> fetch(String url) {
        try {
            String key = Bundles.sha256(url.getBytes(StandardCharsets.UTF_8));
            Path file = CatalogIo.cacheDir().resolve("thumbs").resolve(key + ".png");
            if (Files.isRegularFile(file)) return CompletableFuture.completedFuture(Files.readAllBytes(file));
            if (!GlobalSettings.online()) return CompletableFuture.completedFuture(null);
            return CatalogIo.THUMB_FLIGHT.run(url, () -> CatalogIo.THUMB_DOWNLOADS.submit(() -> CatalogIo.fetch(url, Net.Kind.BULK)))
                    .thenApplyAsync(body -> cache(file, body), CatalogIo.EXECUTOR);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static byte[] cache(Path file, byte[] body) {
        if (body.length > 1 << 20) return null;
        try {
            Files.createDirectories(file.getParent());
            CatalogIo.write(file, body);
        } catch (IOException e) {
            Vibey.LOG.debug("Thumbnail not cached: {}", e.toString());
        }
        return body;
    }
}

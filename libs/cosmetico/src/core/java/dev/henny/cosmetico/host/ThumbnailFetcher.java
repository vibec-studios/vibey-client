package dev.henny.cosmetico.host;

import java.util.concurrent.CompletableFuture;

public interface ThumbnailFetcher {
    CompletableFuture<byte[]> fetch(String url);

    ThumbnailFetcher NONE = url -> CompletableFuture.completedFuture(null);
}

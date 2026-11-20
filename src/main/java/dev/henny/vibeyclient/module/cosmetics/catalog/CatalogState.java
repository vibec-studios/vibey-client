package dev.henny.vibeyclient.module.cosmetics.catalog;

import dev.henny.cosmetico.model.Bundle;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class CatalogState {
    private CatalogState() {}

    record Applied(int gen, String etag, boolean complete) {}

    static final AtomicBoolean RUNNING = new AtomicBoolean();
    static final AtomicInteger GENERATION = new AtomicInteger();
    static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    static volatile long lastCatalog;
    static volatile long nextCatalog;
    static volatile Map<String, Bundle> parsed = Map.of();
    static volatile Applied applied;
    static volatile Set<String> lastIds;
    static volatile String catalogVersion;
    static volatile Consumer<Set<String>> onNewItems = ids -> {};
    static boolean loaded;
    static boolean confirmed;
    static String pendingReadyVersion;
}

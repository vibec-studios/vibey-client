package dev.henny.vibeyclient.backend.net;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * De-duplicates concurrent work by key: while a task for a key runs, further calls get the same future instead of
 * starting it again (same asset hash, same skin URL, meta asked by three places at once). Callers must not cancel
 * the returned future (it is shared).
 */
public final class SingleFlight<K, V> {
    private final ConcurrentHashMap<K, CompletableFuture<V>> inflight = new ConcurrentHashMap<>();

    public CompletableFuture<V> run(K key, Supplier<CompletableFuture<V>> task) {
        CompletableFuture<V> mine = new CompletableFuture<>();
        CompletableFuture<V> running = inflight.putIfAbsent(key, mine);
        if (running != null) return running;
        CompletableFuture<V> f;
        try {
            f = task.get();
        } catch (Throwable t) {
            f = CompletableFuture.failedFuture(t);
        }
        f.whenComplete((v, e) -> {
            inflight.remove(key, mine);
            if (e != null) mine.completeExceptionally(Net.cause(e));
            else mine.complete(v);
        });
        return mine;
    }

    /** Number of keys in flight (tests). */
    public int running() {
        return inflight.size();
    }
}

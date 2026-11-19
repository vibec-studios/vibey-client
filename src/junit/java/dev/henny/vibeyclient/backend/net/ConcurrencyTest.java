package dev.henny.vibeyclient.backend.net;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link Limiter} and {@link SingleFlight}: the coordinators behind parallel asset/thumbnail downloads. */
class ConcurrencyTest {
    @Test
    void limiterRunsAtMostMaxAndKeepsOrder() {
        Limiter l = new Limiter(2);
        List<CompletableFuture<Integer>> gates = new ArrayList<>();
        List<Integer> started = new ArrayList<>();
        List<CompletableFuture<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int n = i;
            CompletableFuture<Integer> gate = new CompletableFuture<>();
            gates.add(gate);
            results.add(l.submit(() -> {
                synchronized (started) {
                    started.add(n);
                }
                return gate;
            }));
        }
        assertEquals(List.of(0, 1), started);
        assertEquals(2, l.running());
        assertEquals(3, l.queued());
        gates.get(1).complete(1);
        assertEquals(List.of(0, 1, 2), started);
        gates.get(0).completeExceptionally(new IllegalStateException("boom")); // a failure frees its slot too
        assertEquals(List.of(0, 1, 2, 3), started);
        gates.get(2).complete(2);
        gates.get(3).complete(3);
        gates.get(4).complete(4);
        assertEquals(List.of(0, 1, 2, 3, 4), started);
        assertEquals(0, l.running());
        assertEquals(0, l.queued());
        assertEquals(4, results.get(4).join());
        CompletionException e = assertThrows(CompletionException.class, () -> results.get(0).join());
        assertEquals("boom", e.getCause().getMessage());
    }

    @Test
    void limiterSurvivesTasksThatThrowOrCompleteImmediately() {
        Limiter l = new Limiter(1);
        AtomicInteger ran = new AtomicInteger();
        List<CompletableFuture<Object>> fs = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            int n = i;
            fs.add(l.submit(() -> {
                ran.incrementAndGet();
                if (n % 3 == 0) throw new IllegalArgumentException("x");
                return CompletableFuture.completedFuture(n);
            }));
        }
        assertEquals(200, ran.get());
        assertEquals(0, l.running());
        assertTrue(fs.get(0).isCompletedExceptionally());
        assertEquals(1, fs.get(1).join());
    }

    @Test
    void singleFlightSharesConcurrentWorkAndForgetsIt() {
        SingleFlight<String, String> sf = new SingleFlight<>();
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<String> gate = new CompletableFuture<>();
        CompletableFuture<String> a = sf.run("k", () -> {
            calls.incrementAndGet();
            return gate;
        });
        CompletableFuture<String> b = sf.run("k", () -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture("other");
        });
        assertSame(a, b);
        assertEquals(1, sf.running());
        gate.complete("v");
        assertEquals("v", b.join());
        assertEquals(0, sf.running());
        // done: the next call runs again
        assertEquals("again", sf.run("k", () -> CompletableFuture.completedFuture("again")).join());
        assertEquals(1, calls.get());
    }

    @Test
    void singleFlightPropagatesFailuresUnwrapped() {
        SingleFlight<String, String> sf = new SingleFlight<>();
        CompletableFuture<String> f = sf.run("k", () -> CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("nope");
        }));
        CompletionException e = assertThrows(CompletionException.class, f::join);
        assertEquals(IllegalStateException.class, e.getCause().getClass());
        assertEquals(0, sf.running());
        CompletableFuture<String> g = sf.run("k", () -> {
            throw new IllegalArgumentException("sync");
        });
        assertTrue(g.isCompletedExceptionally());
        assertEquals(0, sf.running());
    }

    @Test
    void backoffStaysWithinBounds() {
        for (int attempt = 0; attempt < 30; attempt++) {
            long full = Math.min(30_000, 1000L << Math.min(attempt, 20));
            for (int i = 0; i < 50; i++) {
                long d = Net.backoff(attempt, 1000, 30_000);
                assertTrue(d >= full / 2 && d <= full, attempt + ": " + d);
            }
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            long base = 500L << attempt;
            for (int i = 0; i < 50; i++) {
                long d = Rest.backoff(attempt);
                assertTrue(d >= base / 2 && d <= base / 2 + base, attempt + ": " + d);
            }
        }
    }
}

package dev.henny.vibeyclient.client.ranks;

import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.backend.net.HttpCache;
import dev.henny.vibeyclient.backend.net.Net;
import dev.henny.vibeyclient.backend.net.SingleFlight;
import dev.henny.vibeyclient.ranks.RanksDoc;
import dev.henny.vibeyclient.ranks.RanksDocJson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

final class RanksCache {
    private RanksCache() {}

    static final String KEY = "ranks";
    static final String PATH = "/api/v1/ranks";
    private static final SingleFlight<String, Void> FLIGHT = new SingleFlight<>();

    static Path dir() {
        return Vibey.platform().gameDir().resolve("vibeyclient").resolve("cache");
    }

    static void loadDisk() {
        Tasks.run(() -> {
            HttpCache.Entry e = new HttpCache(dir()).load(KEY);
            RanksDoc d = e == null ? null : parse(e.body());
            if (d != null && Ranks.doc().version().isEmpty()) Tasks.onMain(() -> Ranks.setDoc(d));
        });
    }

    static CompletableFuture<Void> fetch(String base) {
        return FLIGHT.run(KEY, () -> {
            HttpCache cache = new HttpCache(dir());
            return CompletableFuture.supplyAsync(() -> cache.load(KEY), Tasks.executor())
                    .thenCompose(cached -> cache.fetch(KEY, base + PATH, cached, Net.Kind.API, Tasks.executor()))
                    .thenAccept(r -> {
                        RanksDoc d = parse(r.body());
                        if (d != null) Tasks.onMain(() -> Ranks.setDoc(d));
                    });
        }).whenComplete((v, err) -> {
            if (err != null) Vibey.LOG.debug("Ranks not loaded: {}", err.toString());
        });
    }

    static RanksDoc parse(byte[] body) {
        try {
            return RanksDocJson.parse(JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (RuntimeException e) {
            return null;
        }
    }
}

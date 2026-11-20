package dev.henny.vibeyclient.client.ranks;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.backend.J;
import dev.henny.vibeyclient.ranks.OwnRanks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RanksSync {
    private RanksSync() {}

    private static final List<AutoCloseable> LISTENERS = new ArrayList<>();
    private static VibeBackend backend;
    private static boolean diskLoaded;

    public static void tick() {
        if (!diskLoaded) {
            diskLoaded = true;
            RanksCache.loadDisk();
        }
        VibeBackend b = GlobalSettings.online() ? Services.get(VibeBackend.class).orElse(null) : null;
        if (b != backend) attach(b);
    }

    private static void attach(VibeBackend b) {
        for (AutoCloseable c : LISTENERS) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        LISTENERS.clear();
        backend = b;
        Ranks.setOwn(OwnRanks.NONE);
        if (b == null) return;
        LISTENERS.add(b.onEvent(BackendClient.EV_READY, RanksSync::ready));
        LISTENERS.add(b.onEvent("me.updated", d -> Ranks.setOwn(OwnRanks.fromMe(d))));
        LISTENERS.add(b.onEvent("ranks.updated", d -> changed(J.str(d, "version"), 5_000)));
        LISTENERS.add(b.onEvent("nametag.updated", d -> PlayerEntry.apply(PlayerEntry.uuid(d.get("userId")), d)));
    }

    private static void ready(JsonObject d) {
        JsonObject user = J.obj(d, "user");
        if (user != null && user.has("ranks")) Ranks.setOwn(OwnRanks.fromMe(user));
        changed(J.str(d, "ranksVersion"), 0);
    }

    private static void changed(String version, int jitterMs) {
        VibeBackend b = backend;
        if (b == null || version == null || version.equals(Ranks.doc().version())) return;
        long delay = jitterMs <= 0 ? 0 : ThreadLocalRandom.current().nextInt(jitterMs);
        Tasks.later(delay, () -> RanksCache.fetch(b.baseUrl()));
    }

    public static void refreshOwn(JsonObject me) {
        if (me != null && me.has("ranks")) Tasks.onMain(() -> Ranks.setOwn(OwnRanks.fromMe(me)));
    }
}

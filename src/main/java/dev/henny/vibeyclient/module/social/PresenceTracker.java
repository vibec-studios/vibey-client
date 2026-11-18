package dev.henny.vibeyclient.module.social;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.backend.J;
import dev.henny.vibeyclient.module.social.model.Model.HostSession;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Our own presence: the status the player chose (online/away/busy/invisible) with auto-away while the window is
 * unfocused, and the activity derived from what the game is doing (menus, a singleplayer world, a server, hosting
 * via {@link HostingService}). Sent over the gateway when it changes (the gateway re-sends it after reconnects).
 */
public final class PresenceTracker {
    public enum Status { ONLINE, AWAY, BUSY, INVISIBLE }

    private final SocialModule module;
    private int ticks;
    private long unfocusedSince;
    private boolean autoAway;
    private String sentStatus;
    private String sentActivity;
    private String worldKey;
    private long startedAt = System.currentTimeMillis();

    PresenceTracker(SocialModule module) {
        this.module = module;
    }

    /** After a fresh {@code ready}: take the status the backend remembers, then send our activity. */
    void onReady() {
        SocialState s = module.service().state;
        if (s.me != null) {
            try {
                Status st = Status.valueOf(s.me.presenceStatus().toUpperCase(Locale.ROOT));
                if (module.status.get() != st) module.status.set(st);
            } catch (IllegalArgumentException ignored) {
            }
        }
        sentStatus = null;
        sentActivity = null;
        update(true);
    }

    void tick() {
        if (++ticks % 20 != 0) return;
        boolean focused = SocialCompat.windowActive();
        long now = System.currentTimeMillis();
        if (focused) {
            unfocusedSince = 0;
            autoAway = false;
        } else if (unfocusedSince == 0) {
            unfocusedSince = now;
        } else if (module.autoAway.on() && now - unfocusedSince > module.autoAwayMinutes.get() * 60_000L) {
            autoAway = true;
        }
        update(false);
    }

    public String effectiveStatus() {
        Status st = module.status.get();
        if (st == Status.ONLINE && autoAway) return "away";
        return st.name().toLowerCase(Locale.ROOT);
    }

    public boolean autoAway() {
        return autoAway;
    }

    /** Forces the next update to send (status changed by the player). */
    public void changed() {
        update(true);
    }

    private void update(boolean force) {
        var backend = module.backend();
        if (backend == null || !backend.isReady()) return;
        String status = effectiveStatus();
        JsonObject activity = activity();
        String act = activity == null ? "null" : activity.toString();
        if (!force && status.equals(sentStatus) && act.equals(sentActivity)) return;
        JsonObject d = new JsonObject();
        if (force || !status.equals(sentStatus)) d.addProperty("status", status);
        if (force || !act.equals(sentActivity)) d.add("activity", activity == null ? com.google.gson.JsonNull.INSTANCE : activity);
        sentStatus = status;
        sentActivity = act;
        backend.sendOp("presence", d);
    }

    private JsonObject activity() {
        if (!module.showPresence.on()) return null;
        String world = SocialCompat.singleplayerWorld();
        String server = SocialCompat.serverAddress();
        String key = world != null ? "w:" + world : server != null ? "s:" + server : "menu";
        if (!key.equals(worldKey)) {
            worldKey = key;
            startedAt = System.currentTimeMillis();
        }
        JsonObject a = J.of("type", "playing", "mcVersion", BuildInfo.MINECRAFT,
                "loader", Vibey.platform().loader().toLowerCase(Locale.ROOT), "joinable", false,
                "startedAt", Instant.ofEpochMilli(startedAt).toString());
        HostingService hs = Services.get(HostingService.class).orElse(null);
        if (world != null && hs != null && hs.isHosting() && hs.address() != null && module.showHosting()) {
            a.addProperty("type", "hosting");
            a.addProperty("world", clip(world, 100));
            a.addProperty("joinable", true);
            JsonObject join = J.of("type", "relay", "address", hs.address(), "port", hs.port() <= 0 ? 25565 : hs.port());
            HostSession mine = module.service().state.myHost;
            if (mine != null && Objects.equals(mine.address(), hs.address())) join.addProperty("hostSessionId", mine.id());
            a.add("join", join);
            return a;
        }
        if (world != null) {
            a.addProperty("world", clip(world, 100));
        } else if (server != null) {
            a.addProperty("server", module.hideServerAddress.on() ? "Multiplayer" : clip(server, 255));
        }
        return a;
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}

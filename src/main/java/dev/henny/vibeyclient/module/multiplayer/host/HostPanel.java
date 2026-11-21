package dev.henny.vibeyclient.module.multiplayer.host;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.module.multiplayer.MultiplayerModule;
import dev.henny.vibeyclient.module.multiplayer.T;
import net.minecraft.client.Minecraft;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class HostPanel {
    private static final String BASE = "/api/v1/hosting/sessions";

    private static volatile VibeBackend bound;
    private static final List<AutoCloseable> handlers = new ArrayList<>();
    private static volatile String sessionId;
    private static volatile PanelModel.State state;
    private static volatile String error = "";
    private static long nextLookupAt;
    private static long lookupDelay = 3_000;
    private static boolean lookingUp;

    private HostPanel() {}

    public static String sessionId() {
        return sessionId;
    }

    public static PanelModel.State state() {
        return state;
    }

    public static String error() {
        return error;
    }

    public static boolean connected() {
        return sessionId != null && state != null;
    }

    public static Integer policyMaxPlayers() {
        PanelModel.State s = state;
        return s == null ? null : s.policy().maxPlayers();
    }

    public static void tick() {
        VibeBackend backend = Services.get(VibeBackend.class).orElse(null);
        if (backend != bound) bind(backend);
        if (!HostSession.isHosting() || HostSession.status() != HostSession.Status.ONLINE) {
            if (sessionId != null || state != null) clear();
            return;
        }
        if (backend == null || !backend.isReady() || sessionId != null || lookingUp) return;
        long now = System.currentTimeMillis();
        if (now < nextLookupAt) return;
        lookup(backend);
    }

    static void clear() {
        sessionId = null;
        state = null;
        error = "";
        nextLookupAt = 0;
        lookupDelay = 3_000;
    }

    private static void bind(VibeBackend backend) {
        for (AutoCloseable c : handlers) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        handlers.clear();
        bound = backend;
        clear();
        if (backend == null) return;
        handlers.add(backend.onEvent("hosting.kick", HostPanel::onKick));
        handlers.add(backend.onEvent("hosting.panel.updated", HostPanel::onPanelUpdated));
        handlers.add(backend.onEvent("hosting.update", HostPanel::onSessionUpdate));
    }

    private static void lookup(VibeBackend backend) {
        lookingUp = true;
        backend.request("GET", BASE, null).whenComplete((res, err) -> Minecraft.getInstance().execute(() -> {
            lookingUp = false;
            String found = err == null ? pick(res, backend.selfId()) : null;
            if (found == null) {
                nextLookupAt = System.currentTimeMillis() + lookupDelay;
                lookupDelay = Math.min(lookupDelay * 2, 30_000);
                return;
            }
            sessionId = found;
            reload();
        }));
    }

    static String pick(JsonElement res, UUID self) {
        if (res == null || !res.isJsonArray()) return null;
        String address = HostSession.hostPart(HostSession.address());
        String fallback = null;
        for (JsonElement e : res.getAsJsonArray()) {
            if (!e.isJsonObject()) continue;
            JsonObject s = e.getAsJsonObject();
            if (!"online".equals(str(s, "status")) || !"world".equals(str(s, "kind"))) continue;
            if (s.has("slot") && s.get("slot").getAsInt() != 0) continue;
            JsonObject host = s.has("host") && s.get("host").isJsonObject() ? s.getAsJsonObject("host") : null;
            if (self != null && host != null && !PanelModel.same(str(host, "id"), null, self.toString(), null)) continue;
            String id = str(s, "id");
            if (id == null) continue;
            if (address != null && address.equals(HostSession.hostPart(str(s, "address")))) return id;
            if (fallback == null) fallback = id;
        }
        return fallback;
    }

    public static void reload() {
        String id = sessionId;
        VibeBackend backend = bound;
        if (id == null || backend == null) return;
        apply(backend.request("GET", BASE + "/" + enc(id) + "/panel", null), id);
    }

    private static void apply(CompletableFuture<JsonElement> call, String id) {
        call.whenComplete((res, err) -> Minecraft.getInstance().execute(() -> {
            if (!id.equals(sessionId)) return;
            if (err != null) {
                VibeBackend.ApiException api = unwrap(err);
                if (api != null && (api.status == 404 || api.status == 410)) {
                    clear();
                    nextLookupAt = System.currentTimeMillis() + 5_000;
                    return;
                }
                error = api != null ? api.getMessage() : T.get("panel.error.network");
                return;
            }
            if (res != null && res.isJsonObject()) {
                state = PanelModel.state(id, res.getAsJsonObject());
                error = "";
            }
        }));
    }

    public static void setPrivacy(String privacy) {
        patchPolicy(dev.henny.vibeyclient.backend.J.of("privacy", privacy));
    }

    public static void setDefaultRole(String role) {
        patchPolicy(dev.henny.vibeyclient.backend.J.of("defaultRole", role));
    }

    public static void setMaxPlayers(int max) {
        patchPolicy(dev.henny.vibeyclient.backend.J.of("maxPlayers", max));
    }

    private static void patchPolicy(JsonObject body) {
        call("PATCH", "/policy", body);
    }

    public static void setRole(String uuid, String name, String role) {
        JsonObject body = dev.henny.vibeyclient.backend.J.of("role", role);
        if (name != null && !name.isBlank()) body.addProperty("name", name);
        call("PUT", "/members/" + enc(player(uuid, name)), body);
    }

    public static void removeMember(String uuid, String name) {
        call("DELETE", "/members/" + enc(player(uuid, name)), null);
    }

    public static void kick(UUID uuid, String name, boolean permanent) {
        String id = sessionId;
        if (id == null || bound == null) {
            Guests.kickNow(uuid, name, null, permanent);
            return;
        }
        JsonObject body = dev.henny.vibeyclient.backend.J.of("player", uuid != null ? uuid.toString() : name, "permanent", permanent);
        CompletableFuture<JsonElement> f = bound.request("POST", BASE + "/" + enc(id) + "/kick", body);
        f.whenComplete((res, err) -> Minecraft.getInstance().execute(() -> {
            if (err != null) MultiplayerModule.LOG.info("Backend kick failed, kicking locally: {}", err.toString());
            Guests.kickNow(uuid, name, null, permanent);
        }));
        apply(f, id);
    }

    public static void unkick(String uuid, String name) {
        call("DELETE", "/kicks/" + enc(player(uuid, name)), null);
    }

    private static void call(String method, String path, JsonObject body) {
        String id = sessionId;
        VibeBackend backend = bound;
        if (id == null || backend == null) return;
        apply(backend.request(method, BASE + "/" + enc(id) + path, body), id);
    }

    public static CompletableFuture<PanelModel.Decision> joinCheck(UUID uuid, String name) {
        String id = sessionId;
        VibeBackend backend = bound;
        if (id == null || backend == null || !backend.isReady()) return CompletableFuture.completedFuture(PanelModel.Decision.OPEN);
        JsonObject body = dev.henny.vibeyclient.backend.J.of("name", name);
        if (uuid != null) body.addProperty("uuid", uuid.toString());
        return backend.request("POST", BASE + "/" + enc(id) + "/join-check", body)
                .orTimeout(8, TimeUnit.SECONDS)
                .thenApply(res -> PanelModel.decision(res != null && res.isJsonObject() ? res.getAsJsonObject() : null))
                .exceptionally(t -> {
                    MultiplayerModule.LOG.info("Join check for {} failed, letting them in: {}", name, t.toString());
                    return PanelModel.Decision.OPEN;
                });
    }

    private static void onKick(JsonObject d) {
        PanelModel.KickOrder order = PanelModel.kick(d);
        VibeBackend backend = bound;
        if (order == null || backend == null || backend.selfId() == null) return;
        if (!PanelModel.same(order.hostId(), null, backend.selfId().toString(), null)) return;
        if (sessionId != null && order.sessionId() != null && !order.sessionId().equals(sessionId)) return;
        if (!HostSession.isHosting()) return;
        UUID uuid = null;
        try {
            if (order.uuid() != null) uuid = UUID.fromString(dashed(order.uuid()));
        } catch (IllegalArgumentException ignored) {
        }
        Guests.kickNow(uuid, order.name(), order.reason(), order.permanent());
    }

    private static void onPanelUpdated(JsonObject d) {
        String id = sessionId;
        if (id == null || !id.equals(str(d, "sessionId"))) return;
        state = PanelModel.state(id, d);
    }

    private static void onSessionUpdate(JsonObject d) {
        String id = str(d, "id");
        if (id == null) return;
        if (id.equals(sessionId) && !"online".equals(str(d, "status"))) {
            clear();
            nextLookupAt = System.currentTimeMillis() + 3_000;
        } else if (sessionId == null && HostSession.isHosting()) {
            nextLookupAt = 0;
        }
    }

    private static String player(String uuid, String name) {
        return uuid != null && !uuid.isBlank() ? uuid : name;
    }

    static String dashed(String uuid) {
        String u = uuid.replace("-", "");
        if (u.length() != 32) return uuid;
        return u.substring(0, 8) + "-" + u.substring(8, 12) + "-" + u.substring(12, 16) + "-" + u.substring(16, 20) + "-" + u.substring(20);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static VibeBackend.ApiException unwrap(Throwable t) {
        while (t != null && !(t instanceof VibeBackend.ApiException)) t = t.getCause();
        return (VibeBackend.ApiException) t;
    }
}

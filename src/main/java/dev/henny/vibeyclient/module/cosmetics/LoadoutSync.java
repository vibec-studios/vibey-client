package dev.henny.vibeyclient.module.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.net.VibeyNet;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.AvatarStates;
import dev.henny.vibeyclient.avatar.ItemLoader;
import dev.henny.vibeyclient.avatar.ItemSource;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.backend.LoadoutKeys;
import dev.henny.vibeyclient.backend.net.Meta;
import dev.henny.vibeyclient.client.VibeyPlayers;
import dev.henny.vibeyclient.net.LoadoutToken;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Cosmetics sync (SPEC §6/§7): our loadout to the backend ({@code PUT /loadout}, debounced) and to the server
 * channel; other players' loadouts from both (merged per player, newest wins) and their emotes. Players without
 * Vibey simply have no entry. Client thread unless noted.
 * <p>Signed loadouts (SPEC §6, VIBE-API-2 §6): the server channel is not trusted. We send our backend-signed token
 * with our loadout; for other players only the slots of a valid token count (plus items everyone has). Without a
 * valid token only {@code default} items are shown and the rest is looked up on the backend.
 */
public final class LoadoutSync {
    private final CosmeticsModule module;

    /** Loadouts of other players by source. */
    private record Remote(Map<String, String> slots, long time) {}

    /**
     * A server channel loadout: the slots the other client claims, its token and the verified token payload (null =
     * missing/invalid).
     */
    private record ServerRemote(Map<String, String> claimed, String token, LoadoutToken.Verified verified, long time) {}

    private final Map<UUID, Remote> fromBackend = new HashMap<>();
    private final Map<UUID, ServerRemote> fromServer = new HashMap<>();
    /** Last valid token per player on this server (its {@code emotes} gate their server-channel emotes). */
    private final Map<UUID, LoadoutToken.Verified> lastValid = new HashMap<>();
    /** Players whose server loadout had unproven items: look them up on the backend (once a minute at most). */
    private final Set<UUID> lookupNeeded = new HashSet<>();
    private final Map<UUID, Long> lookedUp = new HashMap<>();
    private final Set<String> loggedFailures = new HashSet<>();
    private int keyVersion = -1;

    // our own signed token
    private String ownToken = "";
    private long ownTokenExp;
    private long nextTokenFetch;
    private boolean tokenFetching;
    /** The backend answered 404 & co. for signed loadouts (not deployed yet): do not ask after every PUT. */
    private boolean signingMissing;
    private long nextMetaFetch;

    // backend session
    private VibeBackend backend;
    private final List<AutoCloseable> listeners = new ArrayList<>();
    private Set<String> owned = Set.of();
    private boolean meLoaded;
    /** {@code /cosmetics/me} answered: {@link #owned} is real. */
    private boolean ownedKnown;
    /** An upload is due but waits for the catalog and the owned list (see {@link #pushLoadout}). */
    private boolean pushPending;
    private Set<UUID> subscribed = Set.of();
    /** Next full lookup (everybody around us + ourselves) for `client`/`badges`, epoch ms. */
    private long nextFullLookup;
    private int tickCounter;

    /** Emote plays from the backend gateway or the server channel: (player, emoteId or null = stop). */
    public BiConsumer<UUID, String> remoteEmote = (u, e) -> {};

    LoadoutSync(CosmeticsModule module) {
        this.module = module;
    }

    // ============================================================================================ ownership

    /** Online items need to be owned; builtin and dev items are always usable locally. */
    public boolean canUse(AvatarItem item) {
        if (item == null || item.needsUpdate()) return false;
        if (item.source() != ItemSource.CATALOG) return true;
        if ("default".equals(item.availability())) return true;
        return owned.contains(item.id());
    }

    public boolean backendReady() {
        return backend != null && backend.isReady();
    }

    // ============================================================================================ tick

    void tick() {
        VibeBackend b = GlobalSettings.online() ? Services.get(VibeBackend.class).orElse(null) : null;
        if (b != backend) attach(b);
        if (backend != null && backend.isReady() && !meLoaded) loadMe();
        if (pushPending && canPush()) pushLoadout();
        if (LoadoutKeys.version() != keyVersion) {
            keyVersion = LoadoutKeys.version();
            reverifyAll();
        }
        if (backendReady() && ownedKnown && !tokenFetching && System.currentTimeMillis() >= nextTokenFetch && tokenDue()) fetchToken();
        if (++tickCounter % 40 == 0) updateSubscriptions();
        applyRemotes();
    }

    private void attach(VibeBackend b) {
        for (AutoCloseable c : listeners) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        listeners.clear();
        backend = b;
        meLoaded = false;
        ownedKnown = false;
        pushPending = false;
        subscribed = Set.of();
        nextFullLookup = 0;
        fromBackend.clear();
        lookedUp.clear();
        boolean hadToken = !ownToken.isEmpty();
        ownToken = "";
        ownTokenExp = 0;
        nextTokenFetch = 0;
        tokenFetching = false;
        signingMissing = false;
        nextMetaFetch = 0;
        if (hadToken && VibeyNet.available()) VibeyNet.sendLoadout(module.sharedSlots(), "");
        VibeyPlayers.clearBackend();
        if (b == null) return;
        listeners.add(b.onEvent("loadout.updated", d -> {
            UUID id = uuid(d.get("userId"));
            if (id == null) return;
            // VIBE-API-2 §5: client/badges also come with loadout updates (absent on older backends)
            if (d.has("client") || d.has("badges"))
                VibeyPlayers.backend(id, d.get("client") instanceof JsonPrimitive c && "vibey".equals(c.getAsString()), badges(d.get("badges")));
            if (id.equals(module.selfId())) return;
            fromBackend.put(id, new Remote(slots(d.get("slots")), System.currentTimeMillis()));
        }));
        // OTA cosmetics (VIBE-API-2 §4): reload the catalog (spread over a few seconds) and what we own (grants)
        listeners.add(b.onEvent("catalog.updated", d -> {
            ItemLoader.requestCatalog(5_000);
            Tasks.debounce("vibeyclient:cosmetics:owned", 2_000, () -> Tasks.onMain(this::reloadOwned));
        }));
        // after a reconnect the catalog may have changed while we were away (ready carries catalogVersion); at start
        // the first catalog load is usually still running: then it is compared once that load is done
        listeners.add(b.onEvent(dev.henny.vibeyclient.backend.BackendClient.EV_READY, d -> {
            String v = d.get("catalogVersion") instanceof JsonPrimitive p ? p.getAsString() : null;
            ItemLoader.readyVersion(v);
        }));
        listeners.add(b.onEvent("emote.played", d -> {
            UUID id = uuid(d.get("userId"));
            if (id == null || id.equals(module.selfId())) return;
            String emote = d.has("emoteId") ? d.get("emoteId").getAsString() : null;
            boolean play = !d.has("action") || "play".equals(d.get("action").getAsString());
            remoteEmote.accept(id, play ? emote : null);
        }));
    }

    /** Owned items changed (grant/revoke, catalog.updated): refresh the list without touching the loadout. */
    void reloadOwned() {
        if (!backendReady() || !ownedKnown) return;
        VibeBackend b = backend;
        b.request("GET", "/api/v1/cosmetics/me", null).whenComplete((res, err) -> Tasks.onMain(() -> {
            if (b != backend || err != null || !(res instanceof JsonObject o)) return;
            Set<String> own = new HashSet<>();
            if (o.get("owned") instanceof JsonArray a) for (JsonElement e : a) if (e.isJsonPrimitive()) own.add(e.getAsString());
            Set<String> fresh = new HashSet<>(own);
            fresh.removeAll(owned);
            if (!own.equals(owned)) nextTokenFetch = 0; // granted emotes are part of the signed token
            owned = own;
            fresh.removeIf(id -> {
                AvatarItem i = AvatarItems.get(id, ItemSource.CATALOG);
                return i == null || "default".equals(i.availability()) || i.needsUpdate();
            });
            if (!fresh.isEmpty()) module.newItems(fresh);
        }));
    }

    private void loadMe() {
        meLoaded = true;
        VibeBackend b = backend;
        fetchMeta(false);
        b.request("GET", "/api/v1/cosmetics/me", null).whenComplete((res, err) -> Tasks.onMain(() -> {
            if (b != backend) return;
            if (err != null) {
                Vibey.LOG.info("cosmetics/me failed: {}", err.toString());
                return;
            }
            JsonObject o = res.getAsJsonObject();
            Set<String> own = new HashSet<>();
            if (o.get("owned") instanceof JsonArray a) for (JsonElement e : a) own.add(e.getAsString());
            owned = own;
            ownedKnown = true;
            if (module.localDirty()) {
                pushLoadout();
            } else if (o.get("loadout") instanceof JsonObject l) {
                module.adoptRemoteLoadout(Loadout.fromJson(l));
            }
            subscribed = Set.of();
            nextFullLookup = 0;
            updateSubscriptions();
        }));
    }

    /** Debounced PUT of our loadout (only items the backend knows we own). */
    void loadoutChanged() {
        // the current token still describes the old loadout until the PUT answers with a new one (then we resend)
        if (VibeyNet.available()) VibeyNet.sendLoadout(module.sharedSlots(), ownToken());
        if (backendReady()) Tasks.debounce("vibeyclient:cosmetics:put", 1200, () -> Tasks.onMain(this::pushLoadout));
    }

    private boolean canPush() {
        return backendReady() && ownedKnown && ItemLoader.catalogConfirmed();
    }

    /**
     * Uploads our loadout. Slots are only sent for items the backend knows (catalog items we may use), so this must
     * wait until the catalog and our owned list are loaded – otherwise every slot looks offline-only and the upload
     * would clear the stored loadout (e.g. right after switching online features back on).
     */
    private void pushLoadout() {
        if (!canPush()) {
            pushPending = backendReady();
            return;
        }
        pushPending = false;
        Loadout l = module.loadout();
        JsonObject body = new JsonObject();
        JsonObject slots = new JsonObject();
        for (Slot s : Slot.all()) {
            String id = l.get(s);
            AvatarItem item = AvatarItems.get(id);
            boolean online = item != null && item.source() != ItemSource.DEV && AvatarItems.get(id, ItemSource.CATALOG) != null
                    && canUse(AvatarItems.get(id, ItemSource.CATALOG));
            slots.add(s.id(), online ? new JsonPrimitive(id) : com.google.gson.JsonNull.INSTANCE);
        }
        body.add("slots", slots);
        JsonArray wheel = new JsonArray();
        for (String id : l.wheel) {
            AvatarItem item = AvatarItems.get(id, ItemSource.CATALOG);
            wheel.add(item != null && canUse(item) ? new JsonPrimitive(id) : com.google.gson.JsonNull.INSTANCE);
        }
        body.add("emoteWheel", wheel);
        VibeBackend b = backend;
        b.request("PUT", "/api/v1/cosmetics/loadout", body).whenComplete((r, e) -> {
            if (e != null) Vibey.LOG.info("Loadout upload failed: {}", e.toString());
            else Tasks.onMain(() -> {
                module.markSynced();
                if (b != backend) return;
                // VIBE-API-2 §6: the PUT answers with a fresh signed token (older backends: fetch it separately)
                if ((!(r instanceof JsonObject o) || !adoptToken(o.get("signed"))) && !signingMissing) nextTokenFetch = 0;
            });
        });
    }

    // ============================================================================================ signed loadouts

    /** Our current signed token ("" = none or expired). */
    String ownToken() {
        return !ownToken.isEmpty() && ownTokenExp > System.currentTimeMillis() / 1000 ? ownToken : "";
    }

    private boolean tokenDue() {
        return ownToken.isEmpty() || ownTokenExp - System.currentTimeMillis() / 1000 < LoadoutToken.REFRESH_BEFORE_SECONDS;
    }

    /** {@code GET /cosmetics/loadout/signed}; 404 (backend without signing) → try again in 30 minutes. */
    private void fetchToken() {
        tokenFetching = true;
        VibeBackend b = backend;
        b.request("GET", "/api/v1/cosmetics/loadout/signed", null).whenComplete((res, err) -> Tasks.onMain(() -> {
            if (b != backend) return;
            tokenFetching = false;
            long now = System.currentTimeMillis();
            if (err != null) {
                VibeBackend.ApiException ae = BackendClient.unwrap(err);
                boolean unsupported = ae.status == 404 || ae.status == 501 || ae.status == 413 || "feature_disabled".equals(ae.code);
                if (unsupported) Vibey.LOG.debug("Signed loadouts not available on this backend ({})", ae.status);
                else Vibey.LOG.info("Signed loadout fetch failed: {}", ae.getMessage());
                nextTokenFetch = now + (unsupported ? 30 * 60_000L : 60_000L);
                signingMissing = unsupported;
                return;
            }
            signingMissing = false;
            if (!adoptToken(res)) nextTokenFetch = now + 5 * 60_000L;
        }));
    }

    /** Takes {@code {token, payload}} and resends our loadout with it. False if there is no usable token. */
    private boolean adoptToken(JsonElement e) {
        if (!(e instanceof JsonObject o) || !(o.get("token") instanceof JsonPrimitive t) || !t.isString()) return false;
        String token = t.getAsString();
        LoadoutToken.Verified p = LoadoutToken.peek(token);
        if (p == null) {
            Vibey.LOG.warn("Backend sent an unreadable signed loadout token");
            return false;
        }
        if (p.exp() <= System.currentTimeMillis() / 1000) return false;
        boolean changed = !token.equals(ownToken);
        ownToken = token;
        ownTokenExp = p.exp();
        // refresh 30 minutes before exp, but never more often than once a minute
        nextTokenFetch = Math.max(System.currentTimeMillis() + 60_000L, (p.exp() - LoadoutToken.REFRESH_BEFORE_SECONDS) * 1000L);
        if (changed && VibeyNet.available()) VibeyNet.sendLoadout(module.sharedSlots(), ownToken());
        return true;
    }

    /**
     * Signing key from {@code /api/v1/meta} (also when a token names a key we do not know: key rotation, {@code fresh}).
     * The shared {@link Meta} hands the key to {@link LoadoutKeys}; a meta the title screen fetched in the last
     * 5 minutes is good enough unless {@code fresh}.
     */
    private void fetchMeta(boolean fresh) {
        if (backend == null || System.currentTimeMillis() < nextMetaFetch) return;
        nextMetaFetch = System.currentTimeMillis() + 5 * 60_000L;
        LoadoutKeys.install();
        Meta.get(backend.baseUrl(), fresh ? 0 : 5 * 60_000L).exceptionally(t -> null);
    }

    private ServerRemote verify(UUID player, Map<String, String> claimed, String token, long time) {
        LoadoutToken.Result res = LoadoutToken.verify(token, LoadoutKeys.current(), player, System.currentTimeMillis() / 1000);
        if (res.ok()) {
            lastValid.put(player, res.token());
            return new ServerRemote(claimed, token, res.token(), time);
        }
        LoadoutToken.Failure f = res.failure();
        if (f == LoadoutToken.Failure.WRONG_KID || f == LoadoutToken.Failure.NO_KEY) fetchMeta(true);
        if (loggedFailures.add(player + "/" + f)) {
            boolean suspicious = f == LoadoutToken.Failure.BAD_SIGNATURE || f == LoadoutToken.Failure.WRONG_UUID || f == LoadoutToken.Failure.MALFORMED;
            if (suspicious) Vibey.LOG.info("Ignoring the loadout token of {} ({})", player, f);
            else Vibey.LOG.debug("No valid loadout token for {} ({})", player, f);
        }
        for (String id : claimed.values()) {
            if (!id.isEmpty() && !isDefault(id)) {
                lookupNeeded.add(player);
                break;
            }
        }
        return new ServerRemote(claimed, token, null, time);
    }

    /** A new signing key: check all stored tokens again. */
    private void reverifyAll() {
        lastValid.clear();
        for (Map.Entry<UUID, ServerRemote> e : fromServer.entrySet()) {
            ServerRemote r = e.getValue();
            e.setValue(verify(e.getKey(), r.claimed(), r.token(), r.time()));
        }
    }

    /** Items everyone has: {@code default} in the catalog, or built in (and not overridden by the catalog). */
    public static boolean isDefault(String id) {
        if (id == null || id.isEmpty()) return false;
        AvatarItem c = AvatarItems.get(id, ItemSource.CATALOG);
        if (c != null) return "default".equals(c.availability());
        return AvatarItems.get(id, ItemSource.BUILTIN) != null;
    }

    /**
     * A server-channel emote of {@code player}: {@code default} emotes always, granted ones only when they are in the
     * player's last valid token (SPEC §6).
     */
    public boolean allowServerEmote(UUID player, String emoteId) {
        if (isDefault(emoteId)) return true;
        LoadoutToken.Verified v = lastValid.get(player);
        return v != null && v.live(System.currentTimeMillis() / 1000) && v.emotes().contains(emoteId);
    }

    /** The slots a server-channel loadout may show: a valid token's slots, plus claimed items everyone has. */
    private static Map<String, String> effective(ServerRemote r, long nowS) {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, String> e : r.claimed().entrySet()) if (isDefault(e.getValue())) out.put(e.getKey(), e.getValue());
        if (r.verified() != null && r.verified().live(nowS)) out.putAll(r.verified().slots());
        return out;
    }

    /** Our emote to everybody: gateway op + server channel. */
    void sendEmote(String emoteId, boolean play) {
        AvatarItem item = AvatarItems.get(emoteId);
        if (item == null || item.isDev()) return; // dev items stay local
        VibeyNet.sendEmote(emoteId, play);
        if (backendReady()) {
            JsonObject d = new JsonObject();
            d.addProperty("emoteId", emoteId);
            d.addProperty("action", play ? "play" : "stop");
            backend.sendOp("emote", d);
        }
    }

    // ============================================================================================ players around us

    private void updateSubscriptions() {
        if (!backendReady()) return;
        Minecraft mc = Minecraft.getInstance();
        UUID self = module.selfId();
        Set<UUID> now = new LinkedHashSet<>();
        if (mc.level != null) {
            for (Player p : mc.level.players()) {
                if (now.size() >= 199) break;
                if (!p.getUUID().equals(self)) now.add(p.getUUID());
            }
        }
        // `client` ("vibey" while connected or seen in the last 2 minutes) and badges change without an event:
        // look everybody up again every ~2 minutes (jittered so many clients do not hit the backend together).
        // Alone (main menu, singleplayer) only our own badges are left to ask for: every ~10 minutes is plenty.
        long ms = System.currentTimeMillis();
        // somebody is around again: back to the 2-minute rhythm (the newcomers are looked up right below anyway)
        if (!now.isEmpty() && subscribed.isEmpty()) nextFullLookup = Math.min(nextFullLookup, ms + 120_000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(30_000));
        boolean full = ms >= nextFullLookup;
        // players whose server-channel loadout had items without a valid token (at most once a minute each)
        Set<UUID> unproven = new LinkedHashSet<>();
        for (UUID u : lookupNeeded) {
            Long last = lookedUp.get(u);
            if (last == null || ms - last > 60_000) unproven.add(u);
        }
        lookupNeeded.clear();
        if (now.equals(subscribed) && !full && unproven.isEmpty()) return;
        List<String> lookup = new ArrayList<>();
        if (full) {
            nextFullLookup = ms + (now.isEmpty() ? 600_000 : 120_000) + java.util.concurrent.ThreadLocalRandom.current().nextInt(30_000);
            if (self != null) lookup.add(self.toString()); // our own badges
            for (UUID u : now) lookup.add(u.toString());
        } else {
            for (UUID u : now) if (!subscribed.contains(u)) lookup.add(u.toString());
        }
        for (UUID u : unproven) {
            lookedUp.put(u, ms);
            if (!lookup.contains(u.toString())) lookup.add(u.toString());
        }
        if (!now.equals(subscribed)) {
            subscribed = now;
            JsonObject sub = new JsonObject();
            JsonArray ids = new JsonArray();
            for (UUID u : now) ids.add(u.toString());
            sub.add("userIds", ids);
            backend.sendOp("cosmetics.subscribe", sub);
        }
        if (lookup.isEmpty()) return;
        JsonObject body = new JsonObject();
        JsonArray list = new JsonArray();
        for (String s : lookup) list.add(s);
        body.add("userIds", list);
        VibeBackend b = backend;
        b.request("POST", "/api/v1/cosmetics/lookup", body).whenComplete((res, err) -> Tasks.onMain(() -> {
            if (err != null || b != backend || !(res instanceof JsonObject o) || !(o.get("users") instanceof JsonObject users)) return;
            long t = System.currentTimeMillis();
            UUID me = module.selfId();
            for (String s : lookup) {
                UUID id = uuid(new JsonPrimitive(s));
                if (id != null && !users.has(s)) VibeyPlayers.backend(id, false, List.of()); // not a Vibe user (any more)
            }
            for (Map.Entry<String, JsonElement> e : users.entrySet()) {
                UUID id = uuid(new JsonPrimitive(e.getKey()));
                if (id == null || !(e.getValue() instanceof JsonObject u)) continue;
                VibeyPlayers.backend(id, u.get("client") instanceof JsonPrimitive c && "vibey".equals(c.getAsString()), badges(u.get("badges")));
                if (id.equals(me)) continue; // our own loadout is local
                Remote old = fromBackend.get(id);
                if (old == null || old.time < t) fromBackend.put(id, new Remote(slots(u.get("slots")), t));
            }
        }));
    }

    private static List<String> badges(JsonElement e) {
        if (!(e instanceof JsonArray a)) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonElement x : a) if (x instanceof JsonPrimitive p && p.isString()) out.add(p.getAsString());
        return out;
    }

    /**
     * Server channel loadout. With a valid token it competes with the backend (newest wins); without one it only
     * shows items everyone has and never replaces what the backend says about the player.
     */
    void serverLoadout(UUID player, Map<String, String> slots, String token) {
        if (player.equals(module.selfId())) return;
        if (slots.isEmpty() && (token == null || token.isEmpty())) {
            fromServer.remove(player);
            return;
        }
        fromServer.put(player, verify(player, Map.copyOf(slots), token == null ? "" : token, System.currentTimeMillis()));
    }

    void worldLeft() {
        fromServer.clear();
        lastValid.clear();
        lookupNeeded.clear();
        subscribed = Set.of();
        if (backendReady()) {
            JsonObject sub = new JsonObject();
            sub.add("userIds", new JsonArray());
            backend.sendOp("cosmetics.subscribe", sub);
        }
    }

    /** Writes the merged loadout of every known player into their avatar state. */
    private void applyRemotes() {
        Set<UUID> ids = new HashSet<>(fromBackend.keySet());
        ids.addAll(fromServer.keySet());
        long nowS = System.currentTimeMillis() / 1000;
        for (UUID id : ids) {
            Remote a = fromBackend.get(id);
            ServerRemote b = fromServer.get(id);
            Map<String, String> slots;
            if (b == null) slots = a.slots;
            else if (a == null) slots = effective(b, nowS);
            else if (b.verified() != null && b.verified().live(nowS) && b.time() > a.time) slots = effective(b, nowS);
            else slots = a.slots; // unproven server data never overrides the backend
            AvatarState st = AvatarStates.get(id);
            java.util.Arrays.fill(st.slots, null);
            for (Map.Entry<String, String> e : slots.entrySet()) {
                Slot s = Slot.of(e.getKey());
                if (s != null) st.slots[s.ordinal()] = e.getValue();
            }
        }
    }

    // ============================================================================================ util

    private static UUID uuid(JsonElement e) {
        if (e == null || !e.isJsonPrimitive()) return null;
        try {
            return UUID.fromString(e.getAsString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Map<String, String> slots(JsonElement e) {
        Map<String, String> m = new HashMap<>();
        if (e instanceof JsonObject o) {
            for (Map.Entry<String, JsonElement> en : o.entrySet()) {
                if (en.getValue().isJsonPrimitive() && Slot.of(en.getKey()) != null) m.put(en.getKey(), en.getValue().getAsString());
            }
        }
        return m;
    }
}

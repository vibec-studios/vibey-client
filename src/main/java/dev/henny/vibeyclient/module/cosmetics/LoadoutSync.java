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
import dev.henny.vibeyclient.client.VibeyPlayers;
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
 */
public final class LoadoutSync {
    private final CosmeticsModule module;

    /** Loadouts of other players by source. */
    private record Remote(Map<String, String> slots, long time) {}

    private final Map<UUID, Remote> fromBackend = new HashMap<>();
    private final Map<UUID, Remote> fromServer = new HashMap<>();

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
        // after a reconnect the catalog may have changed while we were away (ready carries catalogVersion)
        listeners.add(b.onEvent(dev.henny.vibeyclient.backend.BackendClient.EV_READY, d -> {
            String v = d.get("catalogVersion") instanceof JsonPrimitive p ? p.getAsString() : null;
            if (v == null || !v.equals(ItemLoader.catalogVersion())) ItemLoader.requestCatalog(10_000);
        }));
        // our submission was approved/rejected (the catalog itself follows with catalog.updated)
        listeners.add(b.onEvent("submission.updated", d -> submissionUpdated.accept(d)));
        listeners.add(b.onEvent("emote.played", d -> {
            UUID id = uuid(d.get("userId"));
            if (id == null || id.equals(module.selfId())) return;
            String emote = d.has("emoteId") ? d.get("emoteId").getAsString() : null;
            boolean play = !d.has("action") || "play".equals(d.get("action").getAsString());
            remoteEmote.accept(id, play ? emote : null);
        }));
    }

    /** Gateway {@code submission.updated} payloads (client thread); set by the devlink module. */
    public static volatile java.util.function.Consumer<JsonObject> submissionUpdated = d -> {};

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
        if (VibeyNet.available()) VibeyNet.sendLoadout(module.sharedSlots());
        if (backendReady()) Tasks.debounce("vibeyclient:cosmetics:put", 1200, () -> Tasks.onMain(this::pushLoadout));
    }

    private boolean canPush() {
        return backendReady() && ownedKnown && ItemLoader.catalogLoaded();
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
        backend.request("PUT", "/api/v1/cosmetics/loadout", body).whenComplete((r, e) -> {
            if (e != null) Vibey.LOG.info("Loadout upload failed: {}", e.toString());
            else Tasks.onMain(module::markSynced);
        });
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
        long ms = System.currentTimeMillis();
        boolean full = ms >= nextFullLookup;
        if (now.equals(subscribed) && !full) return;
        List<String> lookup = new ArrayList<>();
        if (full) {
            nextFullLookup = ms + 120_000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(30_000);
            if (self != null) lookup.add(self.toString()); // our own badges
            for (UUID u : now) lookup.add(u.toString());
        } else {
            for (UUID u : now) if (!subscribed.contains(u)) lookup.add(u.toString());
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

    /** Server channel loadout (newest wins against the backend). */
    void serverLoadout(UUID player, Map<String, String> slots) {
        if (player.equals(module.selfId())) return;
        if (slots.isEmpty()) fromServer.remove(player);
        else fromServer.put(player, new Remote(Map.copyOf(slots), System.currentTimeMillis()));
    }

    void worldLeft() {
        fromServer.clear();
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
        for (UUID id : ids) {
            Remote a = fromBackend.get(id), b = fromServer.get(id);
            Remote r = a == null ? b : b == null ? a : (a.time >= b.time ? a : b);
            AvatarState st = AvatarStates.get(id);
            java.util.Arrays.fill(st.slots, null);
            for (Map.Entry<String, String> e : r.slots.entrySet()) {
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

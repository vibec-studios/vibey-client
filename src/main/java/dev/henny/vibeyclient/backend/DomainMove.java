package dev.henny.vibeyclient.backend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Domain move henny.dev → axolite (vibe-launcher docs/DOMAINS.md, client 0.3.0). The old addresses stay in service
 * with the same server and database, so everything stored for them is carried over once: the old default backend
 * and relay in the settings files, the logins in {@code auth.json} and the cached loadout signing key. Values the
 * player set themselves stay. Pure logic on JSON, no game classes (JUnit: DomainMoveTest).
 */
public final class DomainMove {
    private DomainMove() {}

    /** Id of this migration in {@code _data.migrations} / {@code auth.json migrations}. */
    public static final String ID = "domain-move-2026-11";

    public static final String OLD_BACKEND = "https://vibe.henny.dev";
    public static final String NEW_BACKEND = "https://vibe.axolite.app";
    /** Relay hosts the 0.2.x clients and Vibey Multiplayer stored (the relay is on port 25565 under every name). */
    public static final List<String> OLD_RELAY_HOSTS = List.of("relay.vibey.henny.dev", "vibey.henny.dev");
    public static final String NEW_RELAY_HOST = "relay.axolite.cc";
    public static final String NEW_RELAY = NEW_RELAY_HOST + ":25565";

    /** The production backend under its old and its new name: dev login (AUTH_DEV_BYPASS) never goes there. */
    public static final List<String> PRODUCTION_HOSTS = List.of("vibe.henny.dev", "vibe.axolite.app");

    /** {@code stored} is exactly the old default backend (trailing slashes/blanks ignored): the new one, else null. */
    public static String migrateBackend(String stored) {
        if (stored == null) return null;
        String s = stored.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s.equalsIgnoreCase(OLD_BACKEND) ? NEW_BACKEND : null;
    }

    /**
     * {@code stored} is the old relay ({@code relay.vibey.henny.dev} or {@code vibey.henny.dev}, without a port or
     * with 25565): the new default {@code relay.axolite.cc:25565}. Another port on the old host keeps that port.
     * Anything else: null (unchanged).
     */
    public static String migrateRelay(String stored) {
        if (stored == null) return null;
        String s = stored.trim().toLowerCase(Locale.ROOT);
        while (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        String host = s, port = null;
        int colon = s.lastIndexOf(':');
        if (colon > 0) {
            host = s.substring(0, colon);
            port = s.substring(colon + 1);
            if (!port.matches("\\d{1,5}")) return null;
        }
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        if (!OLD_RELAY_HOSTS.contains(host)) return null;
        if (port == null || Integer.parseInt(port) == 25565) return NEW_RELAY;
        return NEW_RELAY_HOST + ":" + Integer.parseInt(port);
    }

    /** True for the production backend under the old or the new domain (any scheme, port or path). */
    public static boolean isProduction(String baseUrl) {
        if (baseUrl == null) return false;
        String host;
        try {
            String s = baseUrl.trim();
            host = URI.create(s.contains("://") ? s : "https://" + s).getHost();
        } catch (RuntimeException e) {
            host = null;
        }
        if (host == null) {
            // unparsable: be strict, a production name anywhere counts
            String l = baseUrl.toLowerCase(Locale.ROOT);
            for (String p : PRODUCTION_HOSTS) if (l.contains(p)) return true;
            return false;
        }
        host = host.toLowerCase(Locale.ROOT);
        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        return PRODUCTION_HOSTS.contains(host);
    }

    // ============================================================================================ settings files

    /** {@code config/vibeyclient/core.json}: {@code backendUrl}. Returns true if a value changed. */
    public static boolean migrateCore(JsonObject json) {
        return replace(json, "backendUrl", DomainMove::migrateBackend);
    }

    /** {@code config/vibeyclient/multiplayer.json}: {@code relay} and {@code _data.host.relay}. */
    public static boolean migrateMultiplayer(JsonObject json) {
        boolean changed = replace(json, "relay", DomainMove::migrateRelay);
        JsonObject data = obj(json, "_data");
        JsonObject host = data == null ? null : obj(data, "host");
        if (host != null) changed |= replace(host, "relay", DomainMove::migrateRelay);
        return changed;
    }

    private static boolean replace(JsonObject o, String key, java.util.function.UnaryOperator<String> fn) {
        JsonElement v = o.get(key);
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) return false;
        String n = fn.apply(v.getAsString());
        if (n == null || n.equals(v.getAsString())) return false;
        o.addProperty(key, n);
        return true;
    }

    // ============================================================================================ auth.json

    /**
     * Renames the logins stored for the old backend ({@code "https://vibe.henny.dev|<account>"}) to the new one in the
     * {@code accounts} object of {@code auth.json}: same server and database, the tokens stay valid. An entry that
     * already exists under the new name wins (the old one is dropped). The old entry is removed, not copied: refresh
     * tokens rotate, keeping a stale copy would only invite reusing it. Returns the number of moved entries.
     */
    public static int migrateAuthAccounts(JsonObject accounts) {
        if (accounts == null) return 0;
        List<String> keys = new ArrayList<>(accounts.keySet());
        int moved = 0;
        for (String k : keys) {
            int bar = k.indexOf('|');
            if (bar <= 0) continue;
            String nb = migrateBackend(k.substring(0, bar));
            if (nb == null) continue;
            String nk = nb + k.substring(bar);
            JsonElement e = accounts.remove(k);
            if (!accounts.has(nk) && e != null) {
                accounts.add(nk, e);
                moved++;
            }
        }
        return moved;
    }

    /** {@code root} of {@code auth.json}: runs {@link #migrateAuthAccounts} once and records it. True if changed. */
    public static boolean migrateAuthFile(JsonObject root) {
        if (done(root, ID)) return false;
        JsonElement a = root.get("accounts");
        if (a != null && a.isJsonObject()) migrateAuthAccounts(a.getAsJsonObject());
        mark(root, ID);
        return true;
    }

    /** Backend URL of a cached loadout signing key: the old one is the same server (same key). */
    public static String cachedBackend(String stored) {
        String n = migrateBackend(stored);
        return n != null ? n : stored;
    }

    // ============================================================================================ bookkeeping

    /** {@code o.migrations} (a JSON array of ids) contains {@code id}. */
    public static boolean done(JsonObject o, String id) {
        JsonElement m = o == null ? null : o.get("migrations");
        if (m == null || !m.isJsonArray()) return false;
        for (JsonElement e : m.getAsJsonArray()) if (e.isJsonPrimitive() && id.equals(e.getAsString())) return true;
        return false;
    }

    public static void mark(JsonObject o, String id) {
        if (done(o, id)) return;
        JsonElement m = o.get("migrations");
        if (m == null || !m.isJsonArray()) {
            m = new com.google.gson.JsonArray();
            o.add("migrations", m);
        }
        m.getAsJsonArray().add(id);
    }

    private static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }
}

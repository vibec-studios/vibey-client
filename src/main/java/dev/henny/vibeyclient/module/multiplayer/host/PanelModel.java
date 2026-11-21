package dev.henny.vibeyclient.module.multiplayer.host;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PanelModel {
    private PanelModel() {}

    public record Policy(String privacy, String defaultRole, Integer maxPlayers) {
        static final Policy DEFAULT = new Policy("public", "guest", null);
    }

    public record Member(String uuid, String name, String role) {}

    public record Kick(String uuid, String name, String reason, boolean permanent) {}

    public record State(String sessionId, Policy policy, List<Member> members, List<Kick> kicks) {
        public String roleOf(String uuid, String name) {
            for (Member m : members) if (same(m.uuid(), m.name(), uuid, name)) return m.role();
            return null;
        }

        public boolean member(String uuid, String name) {
            return "member".equals(roleOf(uuid, name));
        }
    }

    public record Decision(boolean allow, String role, String code, String message) {
        static final Decision OPEN = new Decision(true, "guest", null, null);
    }

    public record KickOrder(String sessionId, String hostId, String uuid, String name, String reason, boolean permanent) {}

    static State state(String sessionId, JsonObject o) {
        JsonObject p = obj(o, "policy");
        Policy policy = p == null ? Policy.DEFAULT : new Policy(str(p, "privacy", "public"), str(p, "defaultRole", "guest"),
                p.has("maxPlayers") && p.get("maxPlayers").isJsonPrimitive() ? p.get("maxPlayers").getAsInt() : null);
        List<Member> members = new ArrayList<>();
        for (JsonObject m : list(o, "members")) {
            members.add(new Member(str(m, "uuid", ""), str(m, "name", "?"), str(m, "role", "guest")));
        }
        List<Kick> kicks = new ArrayList<>();
        for (JsonObject k : list(o, "kicks")) {
            kicks.add(new Kick(str(k, "uuid", null), str(k, "name", "?"), str(k, "reason", null), bool(k, "permanent")));
        }
        return new State(sessionId, policy, List.copyOf(members), List.copyOf(kicks));
    }

    static Decision decision(JsonObject o) {
        if (o == null || !o.has("allow")) return Decision.OPEN;
        if (bool(o, "allow")) return new Decision(true, str(o, "role", "guest"), null, null);
        return new Decision(false, null, str(o, "code", "kicked"), str(o, "message", ""));
    }

    static KickOrder kick(JsonObject o) {
        JsonObject player = obj(o, "player");
        if (player == null) return null;
        return new KickOrder(str(o, "sessionId", null), str(o, "hostId", null), str(player, "uuid", null),
                str(player, "name", ""), str(o, "reason", null), bool(o, "permanent"));
    }

    static boolean same(String uuidA, String nameA, String uuidB, String nameB) {
        String a = norm(uuidA);
        String b = norm(uuidB);
        if (a != null && b != null) return a.equals(b);
        return nameA != null && nameB != null && nameA.equalsIgnoreCase(nameB);
    }

    static String norm(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        return uuid.replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static List<JsonObject> list(JsonObject o, String key) {
        List<JsonObject> out = new ArrayList<>();
        JsonElement e = o == null ? null : o.get(key);
        if (e != null && e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) if (x.isJsonObject()) out.add(x.getAsJsonObject());
        return out;
    }

    private static String str(JsonObject o, String key, String def) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    private static boolean bool(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() && e.getAsBoolean();
    }
}

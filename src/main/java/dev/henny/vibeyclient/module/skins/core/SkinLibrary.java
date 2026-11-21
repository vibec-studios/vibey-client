package dev.henny.vibeyclient.module.skins.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public record SkinLibrary(String accountUuid, String accountName, boolean canChange, String reason, String activeId,
                          String activeSha1, boolean activeSlim, List<LibrarySkin> skins, long updatedAt) {
    public static final int SCHEMA = 1;
    public static final int MAX_SKINS = 500;
    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Pattern FILE = Pattern.compile("^[A-Za-z0-9_-]{1,64}[.]png$");

    public LibrarySkin find(String id) {
        for (LibrarySkin s : skins) if (s.id().equals(id)) return s;
        return null;
    }

    public boolean isActive(LibrarySkin s) {
        if (activeId != null) return activeId.equals(s.id());
        return activeSha1 != null && activeSha1.equalsIgnoreCase(s.sha1());
    }

    public SkinLibrary withActive(String id, boolean slim) {
        return new SkinLibrary(accountUuid, accountName, canChange, reason, id, null, slim, skins, updatedAt);
    }

    public static SkinLibrary parse(String json) {
        JsonObject o;
        try {
            JsonElement e = JsonParser.parseString(json);
            if (!e.isJsonObject()) return null;
            o = e.getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }
        int schema = Json.integer(o, "schema", 0);
        if (schema < 1 || schema > SCHEMA) return null;
        JsonObject acc = Json.object(o, "account");
        String uuid = acc == null ? null : Json.str(acc, "uuid");
        String name = acc == null ? null : Json.str(acc, "name");
        boolean can = Json.bool(o, "can_change", false);
        String reason = Json.str(o, "reason");
        JsonObject active = Json.object(o, "active");
        String activeId = active == null ? null : Json.str(active, "library_id");
        String activeSha = active == null ? null : Json.str(active, "sha1");
        boolean activeSlim = active != null && "slim".equals(Json.str(active, "variant"));
        List<LibrarySkin> list = new ArrayList<>();
        JsonElement arr = o.get("skins");
        if (arr != null && arr.isJsonArray()) {
            for (JsonElement e : arr.getAsJsonArray()) {
                if (list.size() >= MAX_SKINS) break;
                if (!e.isJsonObject()) continue;
                JsonObject s = e.getAsJsonObject();
                String id = Json.str(s, "id");
                String file = Json.str(s, "file");
                if (id == null || file == null || !ID.matcher(id).matches() || !FILE.matcher(file).matches()) continue;
                String n = Json.str(s, "name");
                if (n == null || n.isBlank()) n = "Skin";
                if (n.length() > 48) n = n.substring(0, 48);
                list.add(new LibrarySkin(id, n, "slim".equals(Json.str(s, "variant")), file, Json.str(s, "sha1"), Json.lng(s, "added_at", 0)));
            }
        }
        return new SkinLibrary(uuid, name, can, reason, activeId, activeSha, activeSlim, List.copyOf(list), Json.lng(o, "updated_at", 0));
    }
}

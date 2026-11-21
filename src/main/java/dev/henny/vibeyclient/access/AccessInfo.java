package dev.henny.vibeyclient.access;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record AccessInfo(AccessStatus status, Integer position, Integer total, String reason) {
    public static AccessInfo of(AccessStatus status) {
        return new AccessInfo(status, null, null, null);
    }

    public static AccessInfo denied(String code) {
        return of(AccessStatus.fromDenial(code));
    }

    public static AccessInfo parse(JsonElement e) {
        if (e == null || !e.isJsonObject()) return of(AccessStatus.NONE);
        JsonObject o = e.getAsJsonObject();
        AccessStatus status = AccessStatus.parse(str(o, "status"));
        Integer position = status == AccessStatus.WAITLIST ? positive(o, "position") : null;
        Integer total = status == AccessStatus.WAITLIST ? positive(o, "total") : null;
        String reason = status == AccessStatus.REJECTED ? str(o, "reason") : null;
        return new AccessInfo(status, position, total, reason == null || reason.isBlank() ? null : reason.trim());
    }

    public boolean approved() {
        return status == AccessStatus.APPROVED;
    }

    private static String str(JsonObject o, String key) {
        JsonElement v = o.get(key);
        return v != null && v.isJsonPrimitive() ? v.getAsString() : null;
    }

    private static Integer positive(JsonObject o, String key) {
        JsonElement v = o.get(key);
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) return null;
        int n = v.getAsInt();
        return n > 0 ? n : null;
    }
}

package dev.henny.vibeyclient.module.skins.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public record SkinResult(String id, Status status, String skinId, boolean slim, String error, String message) {
    public enum Status { WORKING, OK, ERROR }

    public static SkinResult parse(String json) {
        try {
            JsonElement e = JsonParser.parseString(json);
            if (!e.isJsonObject()) return null;
            JsonObject o = e.getAsJsonObject();
            int schema = Json.integer(o, "schema", 0);
            if (schema < 1 || schema > SkinRequest.SCHEMA) return null;
            String id = Json.str(o, "id");
            String st = Json.str(o, "status");
            if (id == null || st == null) return null;
            Status status = switch (st) {
                case "working" -> Status.WORKING;
                case "ok" -> Status.OK;
                case "error" -> Status.ERROR;
                default -> null;
            };
            if (status == null) return null;
            String msg = Json.str(o, "message");
            if (msg != null && msg.length() > 200) msg = msg.substring(0, 200);
            return new SkinResult(id, status, Json.str(o, "skin_id"), "slim".equals(Json.str(o, "variant")), Json.str(o, "error"), msg);
        } catch (RuntimeException e) {
            return null;
        }
    }
}

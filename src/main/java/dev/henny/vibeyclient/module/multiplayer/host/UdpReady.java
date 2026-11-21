package dev.henny.vibeyclient.module.multiplayer.host;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public record UdpReady(String token, int tunnelPort, String voiceHost, String voiceAddress, String bedrockHost,
                       int bedrockPort, Map<String, String> denied) {

    public boolean open() {
        return token != null && token.length() == 32;
    }

    public boolean voice() {
        return open() && voiceHost != null;
    }

    public static UdpReady parse(JsonObject msg) {
        String token = str(msg, "token");
        int tunnelPort = num(obj(msg, "tunnel"), "port", 0);
        JsonObject channels = obj(msg, "channels");
        JsonObject voice = obj(channels, "voice");
        JsonObject bedrock = obj(channels, "bedrock");
        Map<String, String> denied = new LinkedHashMap<>();
        JsonObject d = obj(msg, "denied");
        if (d != null) {
            for (Map.Entry<String, JsonElement> e : d.entrySet()) {
                String code = e.getValue().isJsonObject() ? str(e.getValue().getAsJsonObject(), "code") : null;
                denied.put(e.getKey(), code == null ? "disabled" : code);
            }
        }
        return new UdpReady(token == null ? "" : token, tunnelPort, voice == null ? null : str(voice, "voiceHost"),
                voice == null ? null : str(voice, "address"), bedrock == null ? null : str(bedrock, "host"),
                bedrock == null ? 0 : num(bedrock, "port", 0), Map.copyOf(denied));
    }

    static byte[] tokenBytes(String hex) {
        byte[] out = new byte[16];
        for (int i = 0; i < 16; i++) out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return out;
    }

    private static JsonObject obj(JsonObject o, String key) {
        if (o == null) return null;
        JsonElement e = o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static String str(JsonObject o, String key) {
        if (o == null) return null;
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static int num(JsonObject o, String key, int def) {
        if (o == null) return def;
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsInt() : def;
    }
}

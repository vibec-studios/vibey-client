package dev.henny.cosmetico.fixtures;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.parse.Bundles;
import dev.henny.cosmetico.version.VersionGate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Fixtures {
    private Fixtures() {}

    public static final VersionGate GATE = VersionGate.fixed("0.3.1");

    public static String png(int w, int h) {
        byte[] b = new byte[32];
        int[] head = {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
        for (int i = 0; i < head.length; i++) b[i] = (byte) head[i];
        for (int k = 0; k < 4; k++) {
            b[16 + k] = (byte) (w >>> (24 - 8 * k));
            b[20 + k] = (byte) (h >>> (24 - 8 * k));
        }
        return Base64.getEncoder().encodeToString(b);
    }

    public static JsonObject cosmetic(String id, String slot) {
        return json("{\"format\":\"vibey\",\"formatVersion\":1,\"kind\":\"cosmetic\",\"id\":\"" + id + "\",\"name\":{\"en\":\"N\",\"de\":\"D\"},"
                + "\"cosmetic\":{\"slot\":\"" + slot + "\"},\"textures\":[{\"id\":\"main\",\"width\":16,\"height\":16,\"png\":\"" + png(16, 16) + "\"}],"
                + "\"model\":{\"bones\":[{\"name\":\"b\",\"attach\":\"body\",\"pivot\":[0,24,0],"
                + "\"cubes\":[{\"from\":[0,0,0],\"to\":[2,2,2],\"uv\":{\"box\":[0,0]}}]}]}}");
    }

    public static JsonObject emote(String id) {
        return json("{\"format\":\"vibey\",\"formatVersion\":1,\"kind\":\"emote\",\"id\":\"" + id + "\",\"name\":{\"en\":\"E\"},"
                + "\"emote\":{\"animation\":\"main\"},\"animations\":{\"main\":{\"length\":1,\"bones\":{\"head\":{\"rotation\":["
                + "{\"t\":0,\"v\":[0,0,0]},{\"t\":1,\"v\":[10,0,0]}]}}}}}");
    }

    public static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    public static Bundle parse(JsonObject o) {
        return Bundles.parse(o.toString().getBytes(StandardCharsets.UTF_8), GATE);
    }

    public static JsonObject animation(JsonObject bundle, String name, String json) {
        if (!bundle.has("animations")) bundle.add("animations", new JsonObject());
        bundle.getAsJsonObject("animations").add(name, json(json));
        return bundle;
    }
}

package dev.henny.cosmetico.parse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.version.VersionGate;

import java.nio.charset.StandardCharsets;

public final class Bundles {
    private Bundles() {}

    public static Bundle parse(byte[] bytes) {
        return parse(bytes, Cosmetico.versionGate());
    }

    public static Bundle parse(JsonObject o) {
        return parse(o, Cosmetico.versionGate());
    }

    public static Bundle parse(byte[] bytes, VersionGate gate) {
        if (bytes.length > Limits.MAX_BYTES) throw new BundleException("file is " + bytes.length + " bytes (max " + Limits.MAX_BYTES + ")");
        JsonElement root;
        try {
            root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw new BundleException("invalid JSON: " + e.getMessage());
        }
        if (!root.isJsonObject()) throw new BundleException("root must be an object");
        return new BundleReader(gate).read(root.getAsJsonObject(), sha256(bytes), bytes.length);
    }

    public static Bundle parse(JsonObject o, VersionGate gate) {
        byte[] bytes = o.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > Limits.MAX_BYTES) throw new BundleException("bundle is " + bytes.length + " bytes (max " + Limits.MAX_BYTES + ")");
        return new BundleReader(gate).read(o, sha256(bytes), bytes.length);
    }

    public static String sha256(byte[] bytes) {
        return Sha256.hex(bytes);
    }
}

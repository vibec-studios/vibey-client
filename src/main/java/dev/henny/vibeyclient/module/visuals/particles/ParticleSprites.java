package dev.henny.vibeyclient.module.visuals.particles;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ParticleSprites {
    private static final Map<String, List<String>> CACHE = new HashMap<>();

    private ParticleSprites() {}

    public static List<String> textures(String particleId) {
        return CACHE.computeIfAbsent(particleId, ParticleSprites::read);
    }

    public static String frame(String particleId, long tick) {
        List<String> t = textures(particleId);
        if (t.isEmpty()) return "";
        return t.get((int) Math.floorMod(tick, (long) t.size()));
    }

    private static List<String> read(String particleId) {
        List<String> out = new ArrayList<>();
        Identifier id = Identifier.tryParse(particleId);
        if (id == null) return out;
        Identifier file = Identifier.fromNamespaceAndPath(id.getNamespace(), "particles/" + id.getPath() + ".json");
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(file);
        if (res.isEmpty()) return out;
        try (Reader r = res.get().openAsReader()) {
            JsonElement json = JsonParser.parseReader(r);
            if (!json.isJsonObject()) return out;
            JsonObject o = json.getAsJsonObject();
            if (!o.has("textures") || !o.get("textures").isJsonArray()) return out;
            for (JsonElement e : o.getAsJsonArray("textures")) {
                Identifier tex = Identifier.tryParse(e.getAsString());
                if (tex != null) out.add(tex.getNamespace() + ":textures/particle/" + tex.getPath() + ".png");
            }
        } catch (Exception broken) {
            out.clear();
        }
        return out;
    }
}

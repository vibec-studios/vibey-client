package dev.henny.vibeyclient.module.media.video;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.anchor.FaceSnap;
import dev.henny.r3.math.Face;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.persist.PlacementCodec;
import dev.henny.r3.persist.PlacementRecord;
import dev.henny.vibeyclient.module.media.video.world.ScreenObjects;

import java.util.ArrayList;
import java.util.List;

public final class LegacyScreens {
    public static final String KEY = "screens";

    private LegacyScreens() {}

    public static List<PlacementRecord> take(JsonObject data, String worldKey) {
        List<PlacementRecord> out = new ArrayList<>();
        JsonObject all = data.getAsJsonObject(KEY);
        if (all == null || worldKey == null || !all.has(worldKey)) return out;
        JsonArray screens = all.getAsJsonArray(worldKey);
        for (JsonElement e : screens) {
            PlacementRecord r = record(e.getAsJsonObject());
            if (r != null) out.add(r);
        }
        all.remove(worldKey);
        if (all.isEmpty()) data.remove(KEY);
        return out;
    }

    static PlacementRecord record(JsonObject o) {
        try {
            if (!o.has("dim") || !o.has("id")) return null;
            Face normal = Face.byName(o.get("normal").getAsString());
            Face up = Face.byName(o.get("up").getAsString());
            if (normal == null || up == null || normal.axis() == up.axis()) return null;
            WorldObject w = new WorldObject(o.get("id").getAsString(), ScreenObjects.KIND).dimension(o.get("dim").getAsString());
            double x = o.get("x").getAsDouble(), y = o.get("y").getAsDouble(), z = o.get("z").getAsDouble();
            w.transform().position(x, y, z).size(o.get("w").getAsFloat(), o.get("h").getAsFloat());
            FaceSnap.orientation(normal, up, w.transform().orientation());
            w.anchor(new BlockFaceAnchor(normal, up, block(x, normal.stepX()), block(y, normal.stepY()), block(z, normal.stepZ())));
            w.fullBright(!o.has("glow") || o.get("glow").getAsBoolean());
            JsonObject data = new JsonObject();
            for (String k : new String[] {"kind", "uri", "glow", "loop", "vol", "shared", "pos"}) if (o.has(k)) data.add(k, o.get(k));
            return PlacementCodec.encode(w, data);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int block(double v, int step) {
        return (int) Math.floor(v - step * 0.5);
    }
}

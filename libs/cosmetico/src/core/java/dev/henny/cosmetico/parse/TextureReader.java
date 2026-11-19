package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Texture;

import java.util.List;

import static dev.henny.cosmetico.parse.Json.bool;
import static dev.henny.cosmetico.parse.Json.num;
import static dev.henny.cosmetico.parse.Json.str;

final class TextureReader {
    private TextureReader() {}

    static Texture read(JsonObject t, List<String> warnings) {
        String id = str(t, "id");
        if (id == null || id.isEmpty()) throw new BundleException("textures[].id is required");
        int w = (int) num(t, "width", 0), h = (int) num(t, "height", 0);
        if (w < 1 || h < 1) throw new BundleException("texture " + id + ": width/height must be positive");
        byte[] png = Json.base64(str(t, "png"), "texture " + id);
        if (png.length < 24 || (png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G')
            throw new BundleException("texture " + id + " is not a PNG");
        int pw = readInt(png, 16), ph = readInt(png, 20);
        if (pw < 1 || ph < 1) throw new BundleException("texture " + id + ": bad PNG header");
        if (pw > Limits.MAX_TEXTURE) throw new BundleException("texture " + id + " is " + pw + " px wide (max " + Limits.MAX_TEXTURE + ")");
        int frames = Math.max(1, Math.round(ph * (float) w / ((float) pw * h)));
        if (frames == 1 && ph > Limits.MAX_TEXTURE) throw new BundleException("texture " + id + " is " + ph + " px high (max " + Limits.MAX_TEXTURE + ")");
        if (ph > Limits.MAX_STRIP) throw new BundleException("texture " + id + " strip is " + ph + " px high (max " + Limits.MAX_STRIP + ")");
        int frameTime = 1;
        int[] order = null;
        boolean interpolate = false;
        if (t.get("animation") instanceof JsonObject a) {
            frameTime = Math.max(1, (int) num(a, "frameTime", 1));
            interpolate = bool(a, "interpolate", false);
            if (a.get("frames") instanceof JsonArray fa && !fa.isEmpty()) order = frames(fa, frames, id);
        } else if (frames > 1) {
            warnings.add("texture " + id + " looks like a strip of " + frames + " frames but has no animation");
        }
        if (order == null) {
            order = new int[frames];
            for (int i = 0; i < frames; i++) order[i] = i;
        }
        return new Texture(id, w, h, png, pw, ph, frames, frameTime, order, interpolate);
    }

    private static int[] frames(JsonArray fa, int frames, String id) {
        int[] order = new int[fa.size()];
        for (int i = 0; i < order.length; i++) {
            int f = fa.get(i).getAsInt();
            if (f < 0 || f >= frames) throw new BundleException("texture " + id + ": frame " + f + " out of range 0.." + (frames - 1));
            order[i] = f;
        }
        return order;
    }

    static int readInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16) | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }
}

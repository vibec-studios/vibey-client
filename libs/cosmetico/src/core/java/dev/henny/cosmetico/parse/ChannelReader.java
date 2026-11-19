package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Channel;
import dev.henny.cosmetico.model.Interp;

import java.util.Arrays;
import java.util.List;

import static dev.henny.cosmetico.parse.Json.num;
import static dev.henny.cosmetico.parse.Json.str;

final class ChannelReader {
    private ChannelReader() {}

    static Channel read(JsonElement el, String where, List<String> warnings) {
        if (!(el instanceof JsonArray arr) || arr.isEmpty()) return null;
        int n = arr.size();
        float[] t = new float[n], v = new float[n * 3], pre = new float[n * 3];
        Interp[] in = new Interp[n];
        float last = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (!(arr.get(i) instanceof JsonObject k)) throw new BundleException(where + ": keyframes must be objects");
            float kt = (float) num(k, "t", Double.NaN);
            if (Float.isNaN(kt)) throw new BundleException(where + ": keyframe without t");
            if (kt < last) throw new BundleException(where + ": keyframes must be sorted by t");
            last = kt;
            t[i] = kt;
            float[] val = Json.vec(k.get("v"), where + " v", Float.NaN);
            if (Float.isNaN(val[0])) throw new BundleException(where + ": keyframe without v");
            System.arraycopy(val, 0, v, i * 3, 3);
            if (k.has("pre")) System.arraycopy(Json.vec(k.get("pre"), where + " pre", 0), 0, pre, i * 3, 3);
            else Arrays.fill(pre, i * 3, i * 3 + 3, Float.NaN);
            in[i] = interp(str(k, "i"), where, warnings);
        }
        return new Channel(t, v, pre, in);
    }

    private static Interp interp(String is, String where, List<String> warnings) {
        if (is == null) return Interp.LINEAR;
        return switch (is) {
            case "linear" -> Interp.LINEAR;
            case "catmullrom" -> Interp.CATMULLROM;
            case "step" -> Interp.STEP;
            case "bezier" -> Interp.BEZIER;
            default -> {
                String w = where + ": unknown interpolation \"" + is + "\", using linear";
                if (!warnings.contains(w)) warnings.add(w);
                yield Interp.LINEAR;
            }
        };
    }
}

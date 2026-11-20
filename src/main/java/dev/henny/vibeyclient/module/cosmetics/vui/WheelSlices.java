package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.registry.Item;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;

import java.util.ArrayList;
import java.util.List;

final class WheelSlices {
    private WheelSlices() {}

    static List<WheelSlice> of(WheelGeometry g, Wardrobe wd) {
        List<WheelSlice> out = new ArrayList<>();
        for (int i = 0; i < WheelGeometry.SLOTS; i++) {
            String id = wd == null ? null : wd.wheel(i);
            Item item = id == null ? null : Cosmetico.items().get(id);
            String name = item != null ? item.name() : id == null ? "" : id.substring(id.indexOf(':') + 1);
            String thumb = item == null ? "" : Thumbs.of(item);
            out.add(new WheelSlice(i, String.valueOf(i + 1), g.sliceX(i), g.sliceY(i), id == null ? "" : id, name, thumb, id == null));
        }
        return out;
    }

    static String signature(Wardrobe wd) {
        StringBuilder sb = new StringBuilder().append(Cosmetico.items().version());
        for (int i = 0; i < WheelGeometry.SLOTS; i++) {
            String id = wd == null ? null : wd.wheel(i);
            Item item = id == null ? null : Cosmetico.items().get(id);
            sb.append('|').append(id).append(item == null ? "" : Thumbs.of(item));
        }
        return sb.toString();
    }
}

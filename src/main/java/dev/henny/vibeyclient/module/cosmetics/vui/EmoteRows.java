package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.cosmetico.registry.Item;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;

import java.util.ArrayList;
import java.util.List;

final class EmoteRows {
    private EmoteRows() {}

    static List<EmoteRow> of(Wardrobe wd, int selected) {
        List<EmoteRow> out = new ArrayList<>();
        for (Item item : ItemList.items(Slots.of("emotes"), "")) {
            int slot = slotOf(wd, item.id());
            boolean locked = wd != null && !wd.canUse(item);
            String tip = item.name() + " · " + ItemList.rarity(item) + (locked ? " · " + reason(wd, item) : "");
            out.add(new EmoteRow(item.id(), item.name(), tip, Thumbs.of(item), ItemList.color(item), locked,
                    slot < 0 ? "" : String.valueOf(slot + 1), slot >= 0 && slot == selected));
        }
        return out;
    }

    static int slotOf(Wardrobe wd, String id) {
        if (wd == null) return -1;
        for (int i = 0; i < Wardrobe.WHEEL_SLOTS; i++) if (id.equals(wd.wheel(i))) return i;
        return -1;
    }

    private static String reason(Wardrobe wd, Item item) {
        String r = wd.lockedReason(item);
        return r == null ? Lang.tr("ui.wheel.locked") : r;
    }
}

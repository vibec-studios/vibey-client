package dev.henny.cosmetico.state;

import dev.henny.cosmetico.model.Slot;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class AvatarLoadouts implements Loadouts {
    private final AvatarRegistry avatars;

    public AvatarLoadouts(AvatarRegistry avatars) {
        this.avatars = avatars;
    }

    @Override
    public void set(UUID player, Map<Slot, String> slots) {
        LoadoutLayers l = avatars.get(player).loadout;
        l.clearBase();
        for (Map.Entry<Slot, String> e : slots.entrySet()) {
            if (e.getKey() != null && e.getValue() != null && !e.getValue().isEmpty()) l.base[e.getKey().ordinal()] = e.getValue();
        }
    }

    @Override
    public void setIds(UUID player, Map<String, String> slots) {
        Map<Slot, String> m = new EnumMap<>(Slot.class);
        for (Map.Entry<String, String> e : slots.entrySet()) {
            Slot s = Slot.of(e.getKey());
            if (s != null) m.put(s, e.getValue());
        }
        set(player, m);
    }

    @Override
    public Map<Slot, String> get(UUID player) {
        Map<Slot, String> out = new EnumMap<>(Slot.class);
        Avatar a = avatars.peek(player);
        if (a == null) return out;
        for (Slot s : Slot.all()) if (a.loadout.base[s.ordinal()] != null) out.put(s, a.loadout.base[s.ordinal()]);
        return out;
    }

    @Override
    public void clear(UUID player) {
        Avatar a = avatars.peek(player);
        if (a != null) a.loadout.clearBase();
    }

    @Override
    public void clearAll() {
        for (Avatar a : avatars.all()) a.loadout.clearBase();
    }

    @Override
    public void override(UUID player, Slot slot, String itemId) {
        avatars.get(player).loadout.override(slot, itemId);
    }

    @Override
    public void clearOverrides(UUID player) {
        Avatar a = avatars.peek(player);
        if (a != null) a.loadout.clearOverrides();
    }
}

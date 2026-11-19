package dev.henny.cosmetico.state;

import dev.henny.cosmetico.model.Slot;

import java.util.Map;
import java.util.UUID;

public interface Loadouts {
    void set(UUID player, Map<Slot, String> slots);

    void setIds(UUID player, Map<String, String> slots);

    Map<Slot, String> get(UUID player);

    void clear(UUID player);

    void clearAll();

    void override(UUID player, Slot slot, String itemId);

    void clearOverrides(UUID player);
}

package dev.henny.vibeyclient.module.cosmetics;

import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.registry.Item;

public interface Wardrobe {
    int WHEEL_SLOTS = 8;

    String equipped(Slot slot);

    void equip(Item item);

    void unequip(Slot slot);

    boolean canUse(Item item);

    String lockedReason(Item item);

    String wheel(int index);

    void setWheel(int index, String emoteId);

    void playEmote(String emoteId);

    void stopEmote();

    String playingEmote();
}

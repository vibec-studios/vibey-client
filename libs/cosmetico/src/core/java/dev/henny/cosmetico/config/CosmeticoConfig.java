package dev.henny.cosmetico.config;

import dev.henny.cosmetico.model.Slot;

import java.util.Arrays;
import java.util.UUID;

public final class CosmeticoConfig {
    public volatile boolean cosmetics = true;
    public volatile boolean showOwn = true;
    public volatile boolean showOthers = true;
    public volatile boolean emotes = true;
    public volatile boolean showOthersEmotes = true;
    public volatile boolean emotesServerOff;
    public volatile boolean hideWithArmor = true;
    public volatile boolean thirdPersonEmotes = true;
    public volatile float emoteVolume = 1f;
    public volatile UUID self;
    public final boolean[] slotShown = new boolean[Slot.count()];
    public final boolean[] slotServerOff = new boolean[Slot.count()];

    public CosmeticoConfig() {
        Arrays.fill(slotShown, true);
    }

    public boolean slotVisible(Slot slot) {
        return slotShown[slot.ordinal()] && !slotServerOff[slot.ordinal()];
    }

    public boolean isSelf(UUID uuid) {
        return uuid != null && uuid.equals(self);
    }
}

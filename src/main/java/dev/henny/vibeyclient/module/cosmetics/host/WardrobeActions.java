package dev.henny.vibeyclient.module.cosmetics.host;

import dev.henny.cosmetico.host.LocalActions;
import dev.henny.cosmetico.registry.Item;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;

final class WardrobeActions implements LocalActions {
    @Override
    public void equip(Item item) {
        Services.get(Wardrobe.class).ifPresent(w -> w.equip(item));
    }

    @Override
    public void playEmote(String emoteId) {
        Services.get(Wardrobe.class).ifPresent(w -> w.playEmote(emoteId));
    }

    @Override
    public void stopEmote() {
        Services.get(Wardrobe.class).ifPresent(Wardrobe::stopEmote);
    }

    @Override
    public String playingEmote() {
        return Services.get(Wardrobe.class).map(Wardrobe::playingEmote).orElse(null);
    }
}

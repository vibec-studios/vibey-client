package dev.henny.cosmetico.host;

import dev.henny.cosmetico.registry.Item;

public interface LocalActions {
    void equip(Item item);

    void playEmote(String emoteId);

    void stopEmote();

    String playingEmote();

    LocalActions NONE = new LocalActions() {
        @Override
        public void equip(Item item) {}

        @Override
        public void playEmote(String emoteId) {}

        @Override
        public void stopEmote() {}

        @Override
        public String playingEmote() {
            return null;
        }
    };
}

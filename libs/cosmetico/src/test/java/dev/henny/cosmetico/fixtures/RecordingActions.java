package dev.henny.cosmetico.fixtures;

import dev.henny.cosmetico.host.LocalActions;
import dev.henny.cosmetico.registry.Item;

import java.util.ArrayList;
import java.util.List;

public final class RecordingActions implements LocalActions {
    public final List<String> calls = new ArrayList<>();
    public String playing;

    @Override
    public void equip(Item item) {
        calls.add("equip " + item.id());
    }

    @Override
    public void playEmote(String emoteId) {
        calls.add("play " + emoteId);
        playing = emoteId;
    }

    @Override
    public void stopEmote() {
        calls.add("stop");
        playing = null;
    }

    @Override
    public String playingEmote() {
        return playing;
    }
}

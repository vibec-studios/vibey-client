package dev.henny.cosmetico.draw;

import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.config.CosmeticoConfig;
import dev.henny.cosmetico.state.Avatar;

public final class EmoteVisibility {
    private EmoteVisibility() {}

    public static EmotePlay visible(Avatar a, CosmeticoConfig config) {
        if (a == null || a.emote == null || !config.emotes) return null;
        if (!a.preview && config.emotesServerOff) return null;
        if (!a.preview && !config.showOthersEmotes && a.uuid != null && !a.uuid.equals(config.self)) return null;
        return a.emote;
    }
}

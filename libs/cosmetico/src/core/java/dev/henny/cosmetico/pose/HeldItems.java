package dev.henny.cosmetico.pose;

import dev.henny.cosmetico.anim.EmotePlay;

public final class HeldItems {
    private HeldItems() {}

    public static boolean hide(EmotePlay visibleEmote) {
        EmotePlay e = visibleEmote;
        return e != null && e.bundle.emote != null && e.bundle.emote.hideHeldItems() && e.weight > 0.01f;
    }
}

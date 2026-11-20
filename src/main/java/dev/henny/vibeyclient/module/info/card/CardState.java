package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.look.LookTarget;
import dev.henny.vibeyclient.module.info.pin.PinnedCards;
import net.minecraft.client.Minecraft;

public final class CardState {
    private static CardData current;

    private CardState() {}

    public static void update(InfoModule m, Minecraft mc) {
        current = null;
        if (!m.cardActive() || PinnedCards.hovered() != null) return;
        if (LookTarget.state() != null) {
            current = BlockCards.read(mc.level, LookTarget.block(), LookTarget.state(), mc.player, m);
        } else if (LookTarget.entity() != null && m.cardMobs.on()) {
            current = EntityCards.read(LookTarget.entity(), mc.player, m);
        }
    }

    public static CardData current() {
        return current;
    }

    public static void clear() {
        current = null;
    }
}

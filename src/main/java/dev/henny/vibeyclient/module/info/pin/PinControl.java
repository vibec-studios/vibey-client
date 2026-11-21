package dev.henny.vibeyclient.module.info.pin;

import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.card.CardData;
import dev.henny.vibeyclient.module.info.card.CardState;
import dev.henny.vibeyclient.module.info.core.HoldGesture;
import dev.henny.vibeyclient.client.ClientCompat;
import net.minecraft.client.Minecraft;

public final class PinControl {
    public static final int HOLD_TICKS = 8;
    private static final HoldGesture GESTURE = new HoldGesture(HOLD_TICKS);
    private static boolean wasHovering;

    private PinControl() {}

    public static void tick(InfoModule m, Minecraft mc) {
        boolean down = m.cardActive() && ClientCompat.screen() == null && m.pinKey.isDown();
        PinnedCard hovered = PinnedCards.hovered();
        if (down && GESTURE.progress() == 0f) wasHovering = hovered != null;
        HoldGesture.Result r = GESTURE.tick(down);
        if (r == HoldGesture.Result.HOLD && !wasHovering) {
            CardData d = CardState.current();
            if (d != null) PinnedCards.pin(d, mc);
        } else if (r == HoldGesture.Result.TAP && hovered != null) {
            PinnedCards.unpin(hovered);
        }
    }

    public static float progress() {
        return wasHovering ? 0f : GESTURE.progress();
    }

    public static void reset() {
        GESTURE.reset();
        wasHovering = false;
    }
}

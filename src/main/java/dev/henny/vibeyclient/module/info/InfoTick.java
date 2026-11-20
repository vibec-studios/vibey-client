package dev.henny.vibeyclient.module.info;

import dev.henny.r3.mc.client.R3Client;
import dev.henny.vibeyclient.module.info.card.CardState;
import dev.henny.vibeyclient.module.info.food.FoodState;
import dev.henny.vibeyclient.module.info.light.ChunkBorders;
import dev.henny.vibeyclient.module.info.light.LightLabel;
import dev.henny.vibeyclient.module.info.look.LookTarget;
import dev.henny.vibeyclient.module.info.pin.PinControl;
import dev.henny.vibeyclient.module.info.pin.PinnedCards;
import dev.henny.vibeyclient.module.info.tnt.TntTimers;
import dev.henny.vibeyclient.module.info.tool.ToolState;
import net.minecraft.client.Minecraft;

public final class InfoTick {
    private static boolean lightForced;

    private InfoTick() {}

    static void tick(InfoModule m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clear();
            return;
        }
        LookTarget.update(mc);
        CardState.update(m, mc);
        ToolState.update(m, mc);
        FoodState.update(m, mc);
        if (!R3Client.started()) return;
        PinControl.tick(m, mc);
        PinnedCards.tick(m, mc);
        LightLabel.tick(m, mc, lightForced);
        TntTimers.tick(m, mc);
    }

    public static boolean lightForced() {
        return lightForced;
    }

    static void toggleLight(InfoModule m) {
        if (m.lightActive()) lightForced = !lightForced;
    }

    static void rulesChanged(InfoModule m) {
        if (!m.cardActive()) PinnedCards.clear();
        if (!m.chunksActive()) ChunkBorders.offIfOurs();
        if (!m.lightActive()) LightLabel.clear();
        if (!m.tntActive()) TntTimers.clear();
    }

    static void clear() {
        LookTarget.clear();
        CardState.clear();
        ToolState.clear();
        FoodState.clear();
        LightLabel.clear();
        TntTimers.clear();
        PinnedCards.clear();
        PinControl.reset();
    }
}

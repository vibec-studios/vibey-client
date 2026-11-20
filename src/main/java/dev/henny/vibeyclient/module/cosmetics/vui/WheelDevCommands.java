package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.dev.DevDriver;
import dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi;

public final class WheelDevCommands {
    private WheelDevCommands() {}

    public static void register() {
        if (Integer.getInteger("vibeyclient.devDriver") == null) return;
        DevDriver.EXTRA.put("wheel", arg -> {
            ClientCompat.openScreen(null);
            WardrobeUi.openWheel(() -> false);
            return "ok";
        });
        DevDriver.EXTRA.put("wheeledit", arg -> {
            ClientCompat.openScreen(null);
            WardrobeUi.openWheelEditor(null);
            return "ok";
        });
        DevDriver.EXTRA.put("wheelhover", arg -> {
            if (!(ClientCompat.screen() instanceof EmoteWheelVui w)) return "err no wheel";
            w.controller().hoverSlot(Integer.parseInt(arg.trim()));
            return "ok " + w.controller().hovered();
        });
    }
}

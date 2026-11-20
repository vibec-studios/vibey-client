package dev.henny.vibeyclient.module.cinematic.afk;

import dev.henny.vibeyclient.module.cinematic.run.Cinematic;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class AfkEditor {
    private AfkEditor() {}

    public static void open(Screen parent) {
        boolean preview = Minecraft.getInstance().level != null && !Cinematic.active() && Cinematic.start(true);
        HudLayout.openEditor(parent, AfkLayout.PROFILE, () -> {
            if (preview) Cinematic.abort();
        });
    }
}

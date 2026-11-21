package dev.henny.vibeyclient.module.hud.vanilla;

import dev.henny.vibeyclient.client.hud.VanillaLayers;
import dev.henny.vibeyclient.client.hud.VanillaLayers.Layer;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

import java.util.function.BooleanSupplier;

public final class VanillaHud {
    private VanillaHud() {}

    public static void register(BooleanSupplier enabled) {
        HudElement boss = HudLayout.register(BossBarElement.build(enabled));
        HudElement title = HudLayout.register(TitleElement.build(enabled));
        VanillaLayers.replace(Layer.BOSS_BAR, (g, vanilla) -> {
            if (!Placed.active(boss)) return false;
            BossBarElement.draw(g, vanilla, boss);
            return true;
        });
        VanillaLayers.replace(Layer.TITLE, (g, vanilla) -> {
            if (!Placed.active(title)) return false;
            TitleElement.draw(g, vanilla, title);
            return true;
        });
        VanillaLayers.replace(Layer.SCOREBOARD, (g, vanilla) -> replaced("hud.scoreboard"));
        VanillaLayers.replace(Layer.PLAYER_LIST, (g, vanilla) -> replaced("hud.tablist"));
    }

    public static boolean replaced(String id) {
        HudElement e = HudLayout.get(id);
        return e != null && e.available() && !HudLayout.hidden(e) && !HudLayout.serverDisabled(e) && !HudLayout.widgetsHiddenNow();
    }
}

package dev.henny.vibeyclient.module.hud.vanilla;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import java.util.function.BooleanSupplier;

public final class VanillaHud {
    private VanillaHud() {}

    public static void register(BooleanSupplier enabled) {
        HudElement boss = HudLayout.register(BossBarElement.build(enabled));
        HudElement title = HudLayout.register(TitleElement.build(enabled));
        HudElementRegistry.replaceElement(VanillaHudElements.BOSS_BAR, vanilla -> (g, tick) -> {
            if (Placed.active(boss)) BossBarElement.draw(g, tick, vanilla, boss);
            else vanilla.render(g, tick);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.TITLE_AND_SUBTITLE, vanilla -> (g, tick) -> {
            if (Placed.active(title)) TitleElement.draw(g, tick, vanilla, title);
            else vanilla.render(g, tick);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, vanilla -> (g, tick) -> {
            if (!replaced("hud.scoreboard")) vanilla.render(g, tick);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.PLAYER_LIST, vanilla -> (g, tick) -> {
            if (!replaced("hud.tablist")) vanilla.render(g, tick);
        });
    }

    public static boolean replaced(String id) {
        HudElement e = HudLayout.get(id);
        return e != null && e.available() && !HudLayout.hidden(e) && !HudLayout.serverDisabled(e) && !HudLayout.widgetsHiddenNow();
    }
}

package dev.henny.vibeyclient.module.cinematic.afk;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.cinematic.CinematicModule;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.TextWidget;
import net.minecraft.client.Minecraft;

final class TitleWidget extends TextWidget {
    @Override
    protected int rate() {
        return 2;
    }

    @Override
    protected void lines(HudPaint p) {
        line(null, text(p.option("content")));
    }

    static String text(String content) {
        Minecraft mc = Minecraft.getInstance();
        if ("name".equals(content) && mc.player != null) return mc.player.getName().getString();
        if ("world".equals(content)) return AfkInfo.place();
        CinematicModule m = CinematicModule.get();
        String own = m == null ? "" : m.title.get().trim();
        return own.isEmpty() ? Lang.tr("cinematic.afk.title") : own;
    }
}

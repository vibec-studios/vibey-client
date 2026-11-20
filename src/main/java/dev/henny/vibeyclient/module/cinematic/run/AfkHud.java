package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.client.VibeyCore;
import dev.henny.vibeyclient.module.cinematic.CinematicModule;
import dev.henny.vibeyclient.module.cinematic.core.plan.Session;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class AfkHud {
    private AfkHud() {}

    public static boolean render(GuiGraphics g) {
        if (!Cinematic.active()) return false;
        Session.Frame f = Cinematic.frame();
        CinematicModule m = CinematicModule.get();
        Gfx gfx = new Gfx(g, Minecraft.getInstance().font);
        double fade = f == null ? 0 : f.fade();
        double bars = f == null ? 0 : f.bars();
        Overlay.fade(gfx, fade);
        if (m != null && m.vignette.on()) Overlay.vignette(g, gfx, bars);
        if (m != null && m.grain.on()) Overlay.grain(g, gfx, bars);
        if (m == null || m.letterbox.on()) Overlay.bars(gfx, bars);
        VibeyCore.onHud(gfx);
        return true;
    }
}

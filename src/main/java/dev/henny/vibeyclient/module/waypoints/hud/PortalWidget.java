package dev.henny.vibeyclient.module.waypoints.hud;

import dev.henny.vibeyclient.module.waypoints.core.Dimensions;
import dev.henny.vibeyclient.module.waypoints.core.NetherCoords;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import dev.henny.vibeyclient.module.waypoints.view.CoordText;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

final class PortalWidget extends TextWidget {
    @Override
    protected void lines(HudPaint p) {
        Player pl = Minecraft.getInstance().player;
        String dim = WorldContext.dimension();
        String other = dim == null ? null : NetherCoords.counterpart(dim);
        if (pl == null || other == null) {
            if (p.preview) line(CoordText.dimensionName(Dimensions.NETHER), "12 -30");
            return;
        }
        Pos q = NetherCoords.convert(WorldContext.feet(pl), dim, other);
        line(CoordText.dimensionName(other), CoordText.xz(q));
    }
}

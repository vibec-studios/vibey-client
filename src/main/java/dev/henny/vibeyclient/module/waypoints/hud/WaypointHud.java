package dev.henny.vibeyclient.module.waypoints.hud;

import dev.henny.vibeyclient.module.waypoints.WaypointIcons;
import dev.henny.vibeyclient.module.waypoints.WaypointsModule;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

public final class WaypointHud {
    public static final String MINIMAP = "waypoints.minimap";
    public static final String PORTAL = "waypoints.portal";

    private WaypointHud() {}

    public static int minimapTiles() {
        return MinimapWidget.lastTiles;
    }

    public static long minimapPaintNanos() {
        return MinimapWidget.lastPaintNanos;
    }

    public static void showMinimap() {
        HudElement e = HudLayout.get(MINIMAP);
        if (e != null && HudLayout.hidden(e)) HudLayout.setHidden(e, false);
    }

    public static void register(WaypointsModule m) {
        HudLayout.register(HudElement.builder(MINIMAP)
                .label("waypoints.hud.minimap")
                .description("waypoints.hud.minimap.desc")
                .icon(WaypointIcons.MAP)
                .category(HudElement.Category.WORLD)
                .rules(WaypointsModule.RULE_MINIMAP)
                .defaults(Anchor.TOP_RIGHT, -4, 4)
                .scale(1f, 0.5f, 3f)
                .visibleWhen(() -> m.isEnabled() && m.minimap.get())
                .widget(MinimapWidget::new)
                .build());
        HudLayout.register(HudElement.builder(PORTAL)
                .label("waypoints.hud.portal")
                .description("waypoints.hud.portal.desc")
                .icon(WaypointIcons.PORTAL)
                .category(HudElement.Category.WORLD)
                .defaults(Anchor.TOP_LEFT, 4, 70)
                .scale(1f, 0.5f, 3f)
                .startHidden()
                .visibleWhen(m::isEnabled)
                .widget(PortalWidget::new)
                .build());
    }
}

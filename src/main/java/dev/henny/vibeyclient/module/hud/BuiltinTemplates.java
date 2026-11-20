package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudStyle;
import dev.henny.vibeyclient.ui.hud.HudTemplates;

final class BuiltinTemplates {
    private BuiltinTemplates() {}

    static void registerAll() {
        HudTemplates.register(new HudTemplates.Template("pvp", "hud.template.pvp", "hud.template.pvp.desc", BuiltinTemplates::pvp));
        HudTemplates.register(new HudTemplates.Template("minimal", "hud.template.minimal", "hud.template.minimal.desc", BuiltinTemplates::minimal));
        HudTemplates.register(new HudTemplates.Template("building", "hud.template.building", "hud.template.building.desc", BuiltinTemplates::building));
        HudTemplates.register(new HudTemplates.Template("streamer", "hud.template.streamer", "hud.template.streamer.desc", BuiltinTemplates::streamer));
    }

    private static HudStyle style(boolean bg, float opacity, HudStyle.Corners corners, boolean border, int accent) {
        HudStyle s = HudStyle.defaults();
        s.background = bg;
        s.bgOpacity = opacity;
        s.corners = corners;
        s.border = border;
        s.accentColor = accent;
        return s;
    }

    static HudLayout.State pvp() {
        return HudLayout.state()
                .global(style(true, 0.55f, HudStyle.Corners.BEVEL, false, 0x8B6CFF))
                .place("hud.fps", Anchor.TOP_LEFT, 4, 4, 1f)
                .place("hud.cps", Anchor.TOP_LEFT, 4, 21, 1f)
                .place("hud.ping", Anchor.TOP_LEFT, 4, 38, 1f)
                .place("hud.keystrokes", Anchor.TOP_LEFT, 4, 57, 1f).style(s -> s.variant = HudStyle.Variant.DETAILED)
                .place("hud.armor", Anchor.RIGHT, -4, 0, 1f).option("mode", "both")
                .place("hud.effects", Anchor.TOP_RIGHT, -4, 4, 1f).style(s -> s.variant = HudStyle.Variant.DETAILED)
                .place("hud.items", Anchor.BOTTOM, 116, -3, 1f).option("item", "minecraft:arrow")
                .place("hud.items.2", Anchor.BOTTOM, 160, -3, 1f).option("item", "minecraft:totem_of_undying")
                .place("info.card", Anchor.TOP, 0, 4, 1f)
                .place("info.toolwarn", Anchor.CENTER, 0, 24, 1f)
                .copies("hud.items", 2)
                .build();
    }

    static HudLayout.State minimal() {
        return HudLayout.state()
                .global(style(false, 0.5f, HudStyle.Corners.SQUARE, false, 0x8B6CFF))
                .place("hud.fps", Anchor.TOP_LEFT, 3, 3, 1f)
                .place("hud.coords", Anchor.TOP_LEFT, 3, 15, 1f)
                .place("hud.clock", Anchor.TOP_RIGHT, -3, 3, 1f)
                .place("hud.effects", Anchor.TOP_RIGHT, -3, 16, 1f)
                .place("info.card", Anchor.TOP, 0, 4, 1f).style(s -> {
                    s.background = true;
                    s.bgOpacity = 0.6f;
                })
                .place("info.toolwarn", Anchor.CENTER, 0, 24, 1f)
                .build();
    }

    static HudLayout.State building() {
        return HudLayout.state()
                .global(style(true, 0.45f, HudStyle.Corners.SQUARE, false, 0x3FE0C5))
                .place("hud.coords", Anchor.TOP_LEFT, 4, 4, 1f).option("biome", "1").style(s -> s.variant = HudStyle.Variant.DETAILED)
                .place("hud.compass", Anchor.TOP, 0, 4, 1f).style(s -> s.variant = HudStyle.Variant.DETAILED)
                .place("hud.time", Anchor.TOP_RIGHT, -4, 4, 1f).option("icon", "1").style(s -> s.variant = HudStyle.Variant.DETAILED)
                .place("hud.clock", Anchor.TOP_RIGHT, -4, 21, 1f).option("icon", "1")
                .place("hud.fps", Anchor.TOP_RIGHT, -4, 38, 1f).option("icon", "1")
                .place("hud.effects", Anchor.TOP_RIGHT, -4, 56, 1f)
                .place("info.card", Anchor.TOP, 0, 22, 1f)
                .place("info.toolwarn", Anchor.CENTER, 0, 24, 1f)
                .build();
    }

    static HudLayout.State streamer() {
        return HudLayout.state()
                .global(style(true, 0.72f, HudStyle.Corners.BEVEL, true, 0xFF5FA8))
                .place("hud.server", Anchor.TOP_LEFT, 4, 4, 1f).option("hideIp", "1").option("icon", "1")
                .place("hud.clock", Anchor.TOP_RIGHT, -4, 4, 1.5f).style(s -> s.paint = HudStyle.TextPaint.RAINBOW)
                .place("hud.fps", Anchor.TOP_RIGHT, -4, 31, 1f)
                .place("hud.effects", Anchor.TOP_RIGHT, -4, 50, 1f)
                .place("hud.keystrokes", Anchor.BOTTOM_RIGHT, -4, -4, 1f).style(s -> s.variant = HudStyle.Variant.DETAILED)
                .place("info.card", Anchor.TOP, 0, 4, 1f)
                .place("info.toolwarn", Anchor.CENTER, 0, 24, 1f)
                .build();
    }
}

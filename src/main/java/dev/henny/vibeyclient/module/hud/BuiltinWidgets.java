package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudIcons;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudOption;
import dev.henny.vibeyclient.ui.hud.HudWidget;

import java.util.function.Supplier;

/** Registers the built-in widgets (ids and ServerRules keys "hud.<name>", see docs/HUD.md). */
final class BuiltinWidgets {
    private BuiltinWidgets() {}

    static final java.util.List<String> RULE_KEYS = java.util.List.of("hud.fps", "hud.cps", "hud.keystrokes",
            "hud.coords", "hud.armor", "hud.effects", "hud.clock", "hud.ping", "hud.server", "hud.speed", "hud.time",
            "hud.items", "hud.memory", "hud.compass");

    private static final HudOption ICON = HudOption.toggle("icon", "hud.opt.icon", false);

    static void registerAll(HudModule m) {
        reg(m, "fps", HudIcons.FPS, HudElement.Category.INFO, Anchor.TOP_LEFT, 4, 4, TextWidgets.Fps::new, true, ICON);
        reg(m, "cps", HudIcons.CPS, HudElement.Category.COMBAT, Anchor.TOP_LEFT, 4, 20, TextWidgets.Cps::new, true,
                HudOption.choice("buttons", "hud.opt.buttons", "both", "both", "left", "right"), ICON);
        reg(m, "keystrokes", HudIcons.KEYS, HudElement.Category.COMBAT, Anchor.LEFT, 4, 0, KeystrokesWidget::new, true,
                HudOption.toggle("mouse", "hud.opt.mouse", true), HudOption.toggle("space", "hud.opt.space", true),
                HudOption.toggle("cps", "hud.opt.keyCps", true));
        reg(m, "coords", HudIcons.COORDS, HudElement.Category.WORLD, Anchor.TOP_LEFT, 4, 36, TextWidgets.Coords::new, true,
                HudOption.toggle("facing", "hud.opt.facing", true), HudOption.toggle("biome", "hud.opt.biome", false), ICON);
        reg(m, "armor", HudIcons.ARMOR, HudElement.Category.COMBAT, Anchor.RIGHT, -4, 0, ArmorWidget::new, false,
                HudOption.choice("mode", "hud.opt.mode", "percent", "percent", "bar", "both", "value"),
                HudOption.choice("layout", "hud.opt.layout", "column", "column", "row"),
                HudOption.toggle("hand", "hud.opt.hand", false));
        reg(m, "effects", HudIcons.POTION, HudElement.Category.COMBAT, Anchor.TOP_RIGHT, -4, 56, EffectsWidget::new, true);
        reg(m, "clock", HudIcons.CLOCK, HudElement.Category.INFO, Anchor.TOP_RIGHT, -4, 4, TextWidgets.Clock::new, true,
                HudOption.choice("format", "hud.opt.format", "24h", "24h", "12h"), HudOption.toggle("seconds", "hud.opt.seconds", false), ICON);
        reg(m, "ping", HudIcons.PING, HudElement.Category.INFO, Anchor.TOP_LEFT, 4, 52, TextWidgets.Ping::new, true, ICON);
        reg(m, "server", HudIcons.SERVER, HudElement.Category.INFO, Anchor.BOTTOM_RIGHT, -4, -4, TextWidgets.Server::new, true,
                HudOption.toggle("hideIp", "hud.opt.hideIp", false), ICON);
        reg(m, "speed", HudIcons.SPEED, HudElement.Category.MOVEMENT, Anchor.BOTTOM, 0, -54, TextWidgets.Speed::new, true,
                HudOption.toggle("vertical", "hud.opt.vertical", false), ICON);
        reg(m, "time", HudIcons.TIME, HudElement.Category.WORLD, Anchor.TOP_RIGHT, -4, 20, TextWidgets.GameTime::new, true, ICON);
        HudLayout.register(base(m, "items", HudIcons.ITEMS, HudElement.Category.COMBAT, Anchor.BOTTOM, 120, -3)
                .options(HudOption.choice("item", "hud.opt.item", "minecraft:arrow", ItemCounterWidget.PRESETS)
                                .display(ItemCounterWidget::displayName)
                                .capture("hud.opt.item.held", ItemCounterWidget::held),
                        HudOption.toggle("zero", "hud.opt.zero", true))
                .duplicable().variants().widget(ItemCounterWidget::new).build());
        reg(m, "memory", HudIcons.MEMORY, HudElement.Category.INFO, Anchor.TOP_RIGHT, -4, 36, TextWidgets.Memory::new, true, ICON);
        HudLayout.register(base(m, "compass", HudIcons.COMPASS, HudElement.Category.WORLD, Anchor.TOP, 0, 4)
                .freeSize(60, 14).variants().widget(CompassWidget::new).build());
    }

    private static void reg(HudModule m, String name, String icon, HudElement.Category cat, Anchor a, int dx, int dy,
                            Supplier<? extends HudWidget> w, boolean variants, HudOption... options) {
        HudElement.Builder b = base(m, name, icon, cat, a, dx, dy).options(options).widget(w);
        if (variants) b.variants();
        HudLayout.register(b.build());
    }

    private static HudElement.Builder base(HudModule m, String name, String icon, HudElement.Category cat, Anchor a, int dx, int dy) {
        return HudElement.builder("hud." + name)
                .label("hud.widget." + name)
                .description("hud.widget." + name + ".desc")
                .icon(icon)
                .category(cat)
                .rules("hud." + (name.equals("keystrokes") ? "keystrokes" : name))
                .defaults(a, dx, dy)
                .scale(1f, 0.5f, 4f)
                .startHidden()
                .visibleWhen(m::isEnabled);
    }
}

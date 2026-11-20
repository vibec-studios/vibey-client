package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.module.hud.score.ScoreboardWidget;
import dev.henny.vibeyclient.module.hud.tab.TabListWidget;
import dev.henny.vibeyclient.module.hud.timer.ChatTimerInput;
import dev.henny.vibeyclient.module.hud.timer.TimerWidget;
import dev.henny.vibeyclient.module.hud.vanilla.VanillaHud;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudOption;

final class ExtraWidgets {
    private ExtraWidgets() {}

    static void registerAll(HudModule m) {
        HudLayout.register(base(m, "timer", "hud_clock", HudElement.Category.INFO, Anchor.TOP, 0, 22).rules("hud.timer")
                .options(HudOption.choice("mode", "hud.opt.mode", "stopwatch", "stopwatch", "countdown", "splits"),
                        HudOption.choice("length", "hud.opt.length", "1m", TimerWidget.LENGTHS).display(v -> v))
                .duplicable().widget(TimerWidget::new).build());
        HudLayout.register(base(m, "scoreboard", "hud_template", HudElement.Category.WORLD, Anchor.RIGHT, -2, 0)
                .options(HudOption.toggle("numbers", "hud.opt.numbers", true), HudOption.toggle("title", "hud.opt.title", true))
                .widget(ScoreboardWidget::new).build());
        HudLayout.register(base(m, "tablist", "friends", HudElement.Category.INFO, Anchor.TOP, 0, 10)
                .options(HudOption.choice("sort", "hud.opt.sort", "vanilla", "vanilla", "name", "ping"),
                        HudOption.toggle("heads", "hud.opt.heads", true), HudOption.toggle("ms", "hud.opt.ms", false))
                .widget(TabListWidget::new).build());
        VanillaHud.register(m::isEnabled);
        ChatTimerInput.register();
    }

    private static HudElement.Builder base(HudModule m, String name, String icon, HudElement.Category cat, Anchor a, int dx, int dy) {
        return HudElement.builder("hud." + name).label("hud.widget." + name).description("hud.widget." + name + ".desc")
                .icon(icon).category(cat).defaults(a, dx, dy).scale(1f, 0.5f, 3f).startHidden().visibleWhen(m::isEnabled);
    }
}

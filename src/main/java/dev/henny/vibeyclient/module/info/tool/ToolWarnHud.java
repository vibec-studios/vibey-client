package dev.henny.vibeyclient.module.info.tool;

import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.InfoRules;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

public final class ToolWarnHud {
    public static final String ID = "info.toolwarn";

    private ToolWarnHud() {}

    public static void register(InfoModule m) {
        HudLayout.register(HudElement.builder(ID)
                .label("info.hud.toolwarn")
                .description("info.hud.toolwarn.desc")
                .icon(Icons.WARN)
                .category(HudElement.Category.COMBAT)
                .rules(InfoRules.TOOL)
                .defaults(Anchor.CENTER, 0, 24)
                .scale(1f, 0.5f, 3f)
                .visibleWhen(() -> m.isEnabled() && m.toolWarn.on())
                .widget(ToolWarnWidget::new)
                .build());
    }
}

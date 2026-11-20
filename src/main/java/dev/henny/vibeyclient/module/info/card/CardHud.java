package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.InfoRules;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

public final class CardHud {
    public static final String ID = "info.card";

    private CardHud() {}

    public static void register(InfoModule m) {
        InfoIcons.init();
        HudLayout.register(HudElement.builder(ID)
                .label("info.hud.card")
                .description("info.hud.card.desc")
                .icon(Icons.INFO)
                .category(HudElement.Category.WORLD)
                .rules(InfoRules.CARD)
                .defaults(Anchor.TOP, 0, 4)
                .scale(1f, 0.5f, 3f)
                .visibleWhen(() -> m.isEnabled() && m.card.on())
                .widget(InfoCardWidget::new)
                .build());
    }
}

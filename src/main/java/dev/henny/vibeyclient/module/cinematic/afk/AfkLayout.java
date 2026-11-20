package dev.henny.vibeyclient.module.cinematic.afk;

import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudIcons;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudOption;
import dev.henny.vibeyclient.ui.hud.HudWidget;

import java.util.Set;
import java.util.function.Supplier;

public final class AfkLayout {
    public static final String PROFILE = "afk";
    public static final String TITLE = "cinematic.title", TIME = "cinematic.afktime", MODE = "cinematic.mode",
            FRIENDS = "cinematic.friends", HINT = "cinematic.hint", CLOCK = "hud.clock", SERVER = "hud.server",
            LYRICS = "media.lyrics.text", NOW_PLAYING = "media.lyrics.bar";

    private static boolean registered;

    private AfkLayout() {}

    public static boolean showing() {
        return PROFILE.equals(HudLayout.profile());
    }

    public static void register() {
        if (registered) return;
        registered = true;
        HudLayout.registerProfile(PROFILE, Set.of(TITLE, TIME, MODE, HINT, CLOCK, LYRICS, NOW_PLAYING));
        widget(TITLE, Icons.STAR, Anchor.BOTTOM_LEFT, 12, -12, 2f, 6f, TitleWidget::new,
                HudOption.choice("content", "cinematic.hud.content", "text", "text", "name", "world"));
        widget(TIME, HudIcons.CLOCK, Anchor.BOTTOM_RIGHT, -12, -20, 1f, 4f, AfkTimeWidget::new);
        widget(MODE, Icons.WAVE, Anchor.BOTTOM_RIGHT, -12, -8, 1f, 4f, ModeWidget::new);
        widget(FRIENDS, Icons.FRIENDS, Anchor.TOP_LEFT, 12, 28, 1f, 4f, FriendsWidget::new);
        widget(HINT, Icons.INFO, Anchor.TOP_LEFT, 12, 14, 1f, 3f, HintWidget::new);
        AfkPresets.register();
    }

    private static void widget(String id, String icon, Anchor a, int dx, int dy, float scale, float max,
                               Supplier<? extends HudWidget> w, HudOption... options) {
        String name = id.substring(id.indexOf('.') + 1);
        HudLayout.register(HudElement.builder(id).label("cinematic.hud." + name).description("cinematic.hud." + name + ".desc")
                .icon(icon).category(HudElement.Category.OTHER).defaults(a, dx, dy).scale(scale, 0.5f, max)
                .visibleWhen(AfkLayout::showing).options(options).widget(w).build());
    }
}

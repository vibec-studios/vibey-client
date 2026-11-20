package dev.henny.vibeyclient.module.cinematic.afk;

import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudStyle;
import dev.henny.vibeyclient.ui.hud.HudTemplates;

import static dev.henny.vibeyclient.module.cinematic.afk.AfkLayout.*;

final class AfkPresets {
    static final String CINEMA = "cinematic.cinema", MUSIC = "cinematic.music", MINIMAL = "cinematic.minimal";
    private static final int WARM = 0xFFB86B;

    private AfkPresets() {}

    static void register() {
        add(CINEMA, AfkPresets::cinema);
        add(MUSIC, AfkPresets::music);
        add(MINIMAL, AfkPresets::minimal);
        HudTemplates.setDefault(PROFILE, CINEMA);
    }

    private static void add(String id, java.util.function.Supplier<HudLayout.State> state) {
        String key = "cinematic.preset." + id.substring(id.indexOf('.') + 1);
        HudTemplates.register(new HudTemplates.Template(id, key, key + ".desc", state, PROFILE));
    }

    private static HudStyle style() {
        HudStyle s = HudStyle.defaults();
        s.background = false;
        s.shadow = true;
        s.accentColor = WARM;
        return s;
    }

    static HudLayout.State cinema() {
        return HudLayout.state().global(style())
                .place(TITLE, Anchor.BOTTOM_LEFT, 14, -12, 2f)
                .place(TIME, Anchor.BOTTOM_RIGHT, -14, -21, 1f)
                .place(MODE, Anchor.BOTTOM_RIGHT, -14, -9, 1f)
                .place(CLOCK, Anchor.TOP_RIGHT, -14, 14, 1.5f)
                .place(HINT, Anchor.TOP_LEFT, 14, 16, 1f).style(s -> s.textColor = 0xFF9A9AA6)
                .place(NOW_PLAYING, Anchor.TOP, 0, 8, 1f)
                .place(LYRICS, Anchor.BOTTOM, 0, -48, 1f)
                .build();
    }

    static HudLayout.State music() {
        return HudLayout.state().global(style())
                .place(LYRICS, Anchor.CENTER, 0, 10, 1.75f)
                .place(NOW_PLAYING, Anchor.TOP, 0, 10, 1.25f)
                .place(TITLE, Anchor.BOTTOM_LEFT, 14, -14, 1.25f)
                .place(CLOCK, Anchor.BOTTOM_RIGHT, -14, -14, 1.25f)
                .place(FRIENDS, Anchor.TOP_LEFT, 14, 16, 1f)
                .build();
    }

    static HudLayout.State minimal() {
        return HudLayout.state().global(style())
                .place(CLOCK, Anchor.TOP_RIGHT, -14, 14, 1.25f)
                .place(TIME, Anchor.BOTTOM_RIGHT, -14, -14, 1f)
                .place(SERVER, Anchor.BOTTOM_LEFT, 14, -14, 1f)
                .build();
    }
}

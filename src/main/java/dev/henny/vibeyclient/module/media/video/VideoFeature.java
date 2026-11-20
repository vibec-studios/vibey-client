package dev.henny.vibeyclient.module.media.video;

import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.media.MediaFeature;
import dev.henny.vibeyclient.module.media.MediaModule;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

public final class VideoFeature extends MediaFeature {
    public enum Others { ASK, ALWAYS, NEVER }

    public enum Quality {
        P360(360), P480(480), P720(720), P1080(1080);

        public final int lines;

        Quality(int lines) {
            this.lines = lines;
        }
    }

    public final ActionSetting open = action("open", this::openScreens);
    public final KeybindSetting key = keybind("screens", Keys.N).onPress(() -> Video.get().actions.screensKey());
    public final EnumSetting<Others> others = choice("others", Others.ASK);
    public final IntSetting volume = intRange("volume", 80, 0, 100, 5).unit("%");
    public final EnumSetting<Quality> quality = choice("quality", Quality.P720).advanced();
    public final IntSetting fps = intRange("fps", 30, 10, 60, 5).advanced();
    public final IntSetting distance = intRange("distance", 48, 16, 128, 8).unit(" m").advanced();
    public final ActionSetting removeAll = action("removeAll", () -> Video.get().actions.removeAllOwn()).advanced();

    private final Video video = new Video(this);

    public final HudElement overlay = hudElement(HudElement.builder("overlay")
            .size(() -> 160, () -> Math.max(24, Math.round(160 / overlayAspect())))
            .defaults(Anchor.TOP_RIGHT, -6, 30)
            .scale(1f, 0.4f, 3f)
            .visibleWhen(() -> video.overlayScreen() != null)
            .render(this::drawOverlay));

    public VideoFeature(MediaModule media) {
        super(media, "video");
    }

    private float overlayAspect() {
        MediaScreen s = video.overlayScreen();
        return s == null ? 16f / 9f : Math.max(0.3f, s.aspect());
    }

    private void drawOverlay(Gfx g, int w, int h, boolean preview) {
        video.overlay.draw(g, w, h, preview);
    }

    public void openScreens() {
        if (ClientCompat.inWorld()) {
            ClientCompat.openScreen(null);
            video.actions.screensKey();
        }
    }

    @Override
    public void onLoad() {
        video.onLoad();
    }

    @Override
    public void onEnable() {
        video.onEnable();
    }

    @Override
    public void onDisable() {
        video.onDisable();
    }

    @Override
    public void tick() {
        video.tick();
    }

    @Override
    public void renderHud(Gfx g) {
        if (video.overlayScreen() != null) HudLayout.draw(g, overlay);
    }
}

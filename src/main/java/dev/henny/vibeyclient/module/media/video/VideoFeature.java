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

/**
 * Video screens as a part of the Media module (Fabric 1.21.11 only for now): screens placed in the world and a
 * picture-in-picture overlay that plays YouTube (official embed), web pages, files and streams, optionally shared
 * with other Vibey Client players. Settings live in media.json as {@code video.*}, strings in media.video.json.
 * The work is done by {@link Video}; see docs/VIDEO.md.
 */
public final class VideoFeature extends MediaFeature {
    /** What happens with screens other players share. */
    public enum Others { ASK, ALWAYS, NEVER }

    public enum Quality {
        P360(360), P480(480), P720(720), P1080(1080);

        public final int lines;

        Quality(int lines) {
            this.lines = lines;
        }
    }

    // Plain settings
    public final ActionSetting open = action("open", this::openScreens);
    public final KeybindSetting key = keybind("screens", Keys.N).onPress(() -> Video.get().openMenu());
    public final EnumSetting<Others> others = choice("others", Others.ASK);
    public final IntSetting volume = intRange("volume", 80, 0, 100, 5).unit("%");
    // Advanced
    public final EnumSetting<Quality> quality = choice("quality", Quality.P720).advanced();
    public final IntSetting fps = intRange("fps", 30, 10, 60, 5).advanced();
    public final IntSetting distance = intRange("distance", 48, 16, 128, 8).unit(" m").advanced();
    public final ActionSetting removeAll = action("removeAll", () -> Video.get().removeAllOwn()).advanced();

    private final Video video = new Video(this);

    /**
     * Picture-in-picture of one screen; moved and scaled in the HUD editor ("media.video.overlay"). Only in the editor
     * while a screen is actually shown as the overlay: a big empty preview box in the corner of every editor view was
     * just clutter. Saved placements are kept as they are.
     */
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
        video.drawOverlay(g, w, h, preview);
    }

    /** The screens key from anywhere (e.g. a Media settings button): new screen or the one you look at. */
    public void openScreens() {
        if (ClientCompat.inWorld()) video.openMenu();
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
        video.renderHud(g);
    }

    @Override
    public void onWorldJoin() {
        video.onWorldJoin();
    }

    @Override
    public void onWorldLeave() {
        video.onWorldLeave();
    }
}

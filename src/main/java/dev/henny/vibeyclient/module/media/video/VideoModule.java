package dev.henny.vibeyclient.module.media.video;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.ui.Gfx;

/**
 * TEMPORARY host for the video screens until the Media module ({@code module.media}) takes {@link VideoFeature}
 * over (docs/VIDEO.md, "Wiring"). Fabric 1.21.11 only; registered from BuiltinModules if the class is in the jar.
 */
public final class VideoModule extends VibeyModule {
    private final VideoSettings cfg = new VideoSettings("video", settings(), this::isEnabled);
    private final VideoFeature video = new Video(cfg);

    public VideoModule() {
        super("video", ModuleCategory.MEDIA);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
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

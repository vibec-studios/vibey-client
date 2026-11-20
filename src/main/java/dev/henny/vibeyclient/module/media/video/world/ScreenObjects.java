package dev.henny.vibeyclient.module.media.video.world;

import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.interact.SizeLimits;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.VideoSurface;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.vui.VuiPanelContent;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.controls.ScreenControls;

public final class ScreenObjects {
    public static final String KIND = "vibeyclient:video";
    public static final String DOCUMENT = "vibeyclient:video/screen";
    public static final SizeLimits LIMITS = new SizeLimits(0.5f, 16f, 0.25f, 16f);
    public static final float LAYOUT_WIDTH = 256f;
    public static final double MAX_DISTANCE = 128;

    private ScreenObjects() {}

    public static InteractionRules rules() {
        return InteractionRules.screen().limits(LIMITS);
    }

    public static WorldObject create(MediaScreen s, String dimension) {
        WorldObject o = new WorldObject(s.id.toString(), KIND).rules(rules()).dimension(dimension);
        o.transform().size(MediaScreen.DEFAULT_WIDTH, MediaScreen.DEFAULT_WIDTH / s.aspect);
        return dress(o, s);
    }

    public static WorldObject dress(WorldObject o, MediaScreen s) {
        o.rules(rules()).owner(s.owner).remote(s.remote).fullBright(s.glowing);
        o.visibility().maxDistance(MAX_DISTANCE);
        if (!(o.content() instanceof VideoSurface)) o.content(surface(s));
        s.object = o;
        fitLayout(o);
        return o;
    }

    public static VideoSurface surface(WorldObject o) {
        return o != null && o.content() instanceof VideoSurface v ? v : null;
    }

    public static VuiPanelContent strip(WorldObject o) {
        VideoSurface v = surface(o);
        return v != null && v.overlay() != null && v.overlay().content() instanceof VuiPanelContent p ? p : null;
    }

    public static void fitLayout(WorldObject o) {
        float ppb = LAYOUT_WIDTH / Math.max(0.05f, o.transform().width());
        if (Math.abs(o.pixelsPerBlock() - ppb) > 0.01f) o.pixelsPerBlock(ppb);
    }

    public static void close(WorldObject o) {
        VuiPanelContent strip = strip(o);
        if (strip != null) strip.close();
    }

    private static VideoSurface surface(MediaScreen s) {
        ScreenControls controls = new ScreenControls(s);
        VuiPanelContent strip = R3Mc.vuiContent(DOCUMENT, controls);
        controls.attach(strip);
        return new VideoSurface().overlay(strip);
    }
}

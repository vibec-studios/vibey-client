package dev.henny.vibeyclient.module.media.lyrics.world;

import dev.henny.r3.anchor.Anchor;
import dev.henny.r3.anchor.ViewFollowAnchor;
import dev.henny.r3.interact.InteractionEvent;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.R3Mod;
import dev.henny.r3.mc.client.WorldListener;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.LyricsFeature;
import dev.henny.vibeyclient.module.media.lyrics.Playback;

public final class WorldText {
    public static final String KIND = "vibeyclient:lyrics";
    public static final int SKY_LAYER = -1000;
    private static final long PAUSE_RESET_MS = 1000;

    private static final LyricsContent content = new LyricsContent();
    private static WorldObject object;
    private static String signature = "";
    private static long hiddenSince;
    private static boolean hooked;

    private WorldText() {}

    public static void hook() {
        if (hooked) return;
        hooked = true;
        R3Mod.whenReady(() -> {
            R3Mc.scene().interaction().listen(WorldText::event);
            R3Mc.onWorld(new WorldListener() {
                @Override
                public void worldLeft(String worldKey) {
                    object = null;
                    signature = "";
                }
            });
        });
    }

    public static void tick() {
        if (R3Mc.worldKey() == null) return;
        LyricsConfig cfg = LyricsConfig.get();
        boolean show = LyricsFeature.running() && cfg.position != LyricsConfig.Position.HUD && Playback.song() != null && !Playback.hidden();
        if (!show) {
            if (object != null) object.visibility().hidden(true);
            if (hiddenSince == 0) hiddenSince = System.currentTimeMillis();
            return;
        }
        if (object == null || R3Mc.scene().get(object.id()) == null) create();
        if (hiddenSince != 0 && System.currentTimeMillis() - hiddenSince > PAUSE_RESET_MS) signature = "";
        hiddenSince = 0;
        object.visibility().hidden(false);
        String now = LyricsPlacement.signature(cfg);
        if (!now.equals(signature)) {
            signature = now;
            LyricsPlacement.apply(object, content, cfg);
        }
        if (LyricsMode.of(cfg.position) == LyricsMode.WORLD) object.transform().size(content.frameWidth(), content.frameHeight());
    }

    private static void create() {
        object = WorldObject.create(KIND).content(content).rules(new InteractionRules().interactive(false))
                .dimension(R3Mc.scene().query().dimension());
        object.visibility().maxDistance(4096).fadeDistance(0);
        R3Mc.add(object);
        signature = "";
    }

    public static void resetView() {
        signature = "";
    }

    public static long drawnFrames() {
        return content.drawn();
    }

    public static WorldObject object() {
        return object;
    }

    public static float[] viewState() {
        Anchor a = object == null ? null : object.anchor();
        if (!(a instanceof ViewFollowAnchor v)) return new float[] {Float.NaN, 0f, 0f};
        return new float[] {v.yaw(), v.elevation(), v.gliding() ? 1f : 0f};
    }

    private static void event(InteractionEvent e) {
        if (object == null || e.target() != object || !e.changedPose()) return;
        LyricsConfig cfg = LyricsConfig.get();
        var p = object.transform().position();
        cfg.anchorX = p.x;
        cfg.anchorY = p.y;
        cfg.anchorZ = p.z;
        cfg.anchorScale = object.transform().scale();
        cfg.anchorSet = true;
        cfg.save();
        signature = LyricsPlacement.signature(cfg);
    }
}

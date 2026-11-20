package dev.henny.vibeyclient.module.media.lyrics.world;

import dev.henny.r3.anchor.Anchor;
import dev.henny.r3.anchor.CameraAnchor;
import dev.henny.r3.anchor.FreeAnchor;
import dev.henny.r3.anchor.LowersWhenLookingDown;
import dev.henny.r3.anchor.SoftFollowAnchor;
import dev.henny.r3.anchor.ViewFollowAnchor;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.interact.SizeLimits;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import net.minecraft.client.Minecraft;

import java.util.Locale;

final class LyricsPlacement {
    static final float FRONT_PITCH = 6f;
    static final float FRONT_SMOOTHING = 4.5f;
    static final SizeLimits WORLD_LIMITS = new SizeLimits(0.25f, 64f, 0.1f, 64f);

    private LyricsPlacement() {}

    static String signature(LyricsConfig c) {
        return String.format(Locale.ROOT, "%s|%s|%d|%d|%.1f|%.2f,%.2f,%.2f|%.2f|%s|%.2f|%d",
                c.position, c.skyMode, c.skyHeight, c.frontDistance, c.skyYaw, c.anchorX, c.anchorY, c.anchorZ, c.anchorYaw,
                c.anchorBillboard, c.anchorScale, skyDistance());
    }

    static void apply(WorldObject o, LyricsContent content, LyricsConfig c) {
        LyricsMode mode = LyricsMode.of(c.position);
        content.mode(mode);
        o.layer(mode == LyricsMode.SKY ? WorldText.SKY_LAYER : 0);
        o.transform().scale(1f);
        boolean world = mode == LyricsMode.WORLD;
        o.rules(world ? new InteractionRules().keepAspect(true).edgeResizable(false).limits(WORLD_LIMITS)
                : new InteractionRules().interactive(false));
        if (world) {
            o.anchor(FreeAnchor.INSTANCE);
            o.transform().position(c.anchorX, c.anchorY, c.anchorZ).scale(Math.max(0.1f, c.anchorScale));
            o.transform().orientation().rotationY((float) Math.PI - (float) Math.toRadians(c.anchorYaw));
            return;
        }
        o.anchor(mode == LyricsMode.FRONT ? new CameraAnchor(c.frontDistance, 0f, FRONT_PITCH, FRONT_SMOOTHING) : sky(c));
    }

    private static Anchor sky(LyricsConfig c) {
        double d = skyDistance();
        return switch (c.skyMode) {
            case VIEW -> new ViewFollowAnchor(d);
            case FOLLOW -> new LowersWhenLookingDown(SoftFollowAnchor.follow(d, c.skyHeight));
            case SOFT -> new LowersWhenLookingDown(SoftFollowAnchor.soft(d, c.skyHeight));
            case FIXED -> SoftFollowAnchor.fixed(d, c.skyYaw, c.skyHeight);
        };
    }

    static int skyDistance() {
        double rd = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;
        return (int) Math.max(32, Math.min(96, rd * 0.35));
    }
}

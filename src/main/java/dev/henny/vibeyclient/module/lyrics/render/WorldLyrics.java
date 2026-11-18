package dev.henny.vibeyclient.module.lyrics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.lyrics.LyricsCompat;
import dev.henny.vibeyclient.module.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.lyrics.Playback;
import dev.henny.vibeyclient.module.lyrics.sky.SkyDome;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Lyrics in the world: huge and far away in the sky, floating in front of you, or pinned to a spot. The frame is a
 * flat billboard turned towards the camera; its size grows with the distance so the sky text stays readable.
 * Called from the world render hook with a pose stack whose origin is the camera position.
 */
public final class WorldLyrics {
    /** "In front of hills": how far away (blocks) the sky lyrics are really drawn. */
    private static final double NEAR = 3.5;
    private static float smoothYaw = Float.NaN;
    private static float smoothPitch;
    private static long lastNanos;
    private static float dt;
    /** SOFT sky mode: the direction the text sits in, and whether it is gliding back in front of you. */
    private static float softYaw = Float.NaN;
    private static boolean recentering;
    /** Frames in which world lyrics were handed to the renderer (the client test checks the hook works). */
    public static long drawnFrames;

    private WorldLyrics() {}

    public static void render(PoseStack pose, WorldSink sink) {
        if (!dev.henny.vibeyclient.module.lyrics.LyricsModule.running()) return;
        SkyDome.render(pose, sink);
        LyricsConfig cfg = LyricsConfig.get();
        Minecraft mc = Minecraft.getInstance();
        if (cfg.position == LyricsConfig.Position.HUD || Playback.song() == null || mc.level == null || Playback.hidden()) return;
        Camera cam = LyricsCompat.camera();
        Vec3 eye = LyricsCompat.cameraPos(cam);
        float camYaw = LyricsCompat.cameraYaw(cam);
        float camPitch = LyricsCompat.cameraPitch(cam);
        smooth(camYaw, camPitch);

        float size = cfg.size / 100f;
        Vec3 target;
        float scale;
        float maxWidth;
        boolean faceCamera = true;
        float fixedYaw = 0;
        switch (cfg.position) {
            case SKY -> {
                // Far, but well inside the fog so it never fades out; scaled with the distance.
                double rd = mc.options.getEffectiveRenderDistance() * 16.0;
                double dist = Math.max(32, Math.min(96, rd * 0.35));
                float yaw = switch (cfg.skyMode) {
                    case FOLLOW -> smoothYaw;
                    case FIXED -> cfg.skyYaw;
                    default -> soft(camYaw);
                };
                // Looking down while you walk pulls the text lower so it stays in view; never below the horizon.
                float height = cfg.skyHeight;
                if (cfg.skyMode != LyricsConfig.SkyMode.FIXED) {
                    height = Math.max(Math.min(2f, height), height - Math.max(0f, smoothPitch));
                }
                target = eye.add(direction(yaw, -height).scale(dist));
                scale = (float) (dist * 0.013 * size);
                maxWidth = 140;
            }
            case FRONT -> {
                double dist = cfg.frontDistance;
                target = eye.add(direction(smoothYaw, smoothPitch + 6f).scale(dist));
                scale = (float) (dist * 0.0075 * size);
                maxWidth = 150;
            }
            default -> {
                if (!cfg.anchorSet) return;
                target = new Vec3(cfg.anchorX, cfg.anchorY, cfg.anchorZ);
                scale = 0.07f * size;
                maxWidth = 220;
                faceCamera = cfg.anchorBillboard;
                fixedYaw = cfg.anchorYaw;
            }
        }
        Vec3 rel = target.subtract(eye);
        float room = (float) (rel.length() / scale);
        LyricLayout.Frame frame = LyricLayout.build(mc.font, Playback.lyrics(), Playback.song().title(),
                Playback.song().artist(), Playback.lyricTimeMs(), maxWidth, room);
        if (frame.isEmpty()) return;

        Quaternionf rot;
        if (faceCamera) {
            double h = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            float yaw = (float) Math.atan2(-rel.x, rel.z);
            float pitch = (float) Math.atan2(-rel.y, h);
            rot = new Quaternionf().rotationYXZ((float) Math.PI - yaw, -pitch, 0f);
        } else {
            rot = new Quaternionf().rotationY((float) Math.PI - (float) Math.toRadians(fixedYaw));
        }

        pose.pushPose();
        if (cfg.position == LyricsConfig.Position.SKY && cfg.skyOnTop) {
            // Shrink everything towards the eye: on screen it looks exactly the same (perspective), but it now sits a
            // few blocks away, in front of hills and trees, and still hides its own far words and 3D sides correctly
            // (switching the depth test off would not: the font's letters are spread over several textures).
            float k = (float) (NEAR / rel.length());
            pose.scale(k, k, k);
        }
        pose.translate(rel.x, rel.y, rel.z);
        LyricsCompat.rotate(pose, rot);
        pose.scale(scale, -scale, scale);
        Font font = mc.font;
        new Painter(font) {
            @Override
            protected void draw(Component c, LyricLayout.Run r, float ox, float oy, float oz, int argb, boolean back) {
                float s = r.scale();
                pose.pushPose();
                pose.translate(r.x(), r.y() + LyricLayout.ROW * s / 2f, r.z());
                if (r.yaw() != 0f || r.roll() != 0f) {
                    LyricsCompat.rotate(pose, new Quaternionf().rotationYXZ((float) Math.toRadians(r.yaw()), 0f, (float) Math.toRadians(r.roll())));
                }
                pose.scale(s, s, s);
                // Glow copies and 3D sides sit a hair behind the text so they never cover it.
                pose.translate(-font.width(c) / 2f + ox, -LyricLayout.ROW / 2f + oy, oz);
                sink.text(font, pose, c, 0, 0, argb, 0, !back);
                pose.popPose();
            }

            @Override
            protected void box(float x1, float y1, float x2, float y2, int argb) {
                // A text background is the one rectangle every version can draw here: stretch one space.
                Component space = Component.literal(" ");
                float w = font.width(space);
                pose.pushPose();
                pose.translate(x1, y1, -0.7f);
                pose.scale((x2 - x1) / (w + 2f), (y2 - y1) / 10f, 1f);
                sink.text(font, pose, space, 1, 1, 0, argb, false);
                pose.popPose();
            }
        }.paint(frame);
        pose.popPose();
        drawnFrames++;
    }

    /** Eases the "follow view" direction so the text floats after your head instead of being glued to it. */
    private static void smooth(float yaw, float pitch) {
        long now = System.nanoTime();
        dt = lastNanos == 0 ? 1f : Math.min(0.25f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        if (Float.isNaN(smoothYaw)) {
            smoothYaw = yaw;
            smoothPitch = pitch;
            return;
        }
        float k = 1f - (float) Math.exp(-dt * 4.5f);
        float d = ((yaw - smoothYaw) % 360f + 540f) % 360f - 180f;
        smoothYaw += d * k;
        smoothPitch += (pitch - smoothPitch) * k;
    }

    /**
     * Lazy follow: the text stays where it is while you look around a bit, and once you turn more than about 30°
     * away it glides back in front of you (like a VR menu), so you always see it, also while walking.
     */
    private static float soft(float camYaw) {
        if (Float.isNaN(softYaw)) softYaw = camYaw;
        float d = wrap(camYaw - softYaw);
        if (Math.abs(d) > 30f) recentering = true;
        if (recentering) {
            softYaw += d * (1f - (float) Math.exp(-dt * 2.6f));
            if (Math.abs(d) < 2.5f) recentering = false;
        }
        softYaw = wrap(softYaw);
        return softYaw;
    }

    private static float wrap(float deg) {
        return ((deg % 360f) + 540f) % 360f - 180f;
    }

    private static Vec3 direction(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double c = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * c, -Math.sin(pitch), Math.cos(yaw) * c);
    }
}

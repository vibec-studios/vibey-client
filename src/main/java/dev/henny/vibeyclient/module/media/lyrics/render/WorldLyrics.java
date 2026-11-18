package dev.henny.vibeyclient.module.media.lyrics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.media.lyrics.LyricsCompat;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.sky.SkyDome;
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
    /** VIEW sky mode: where the text sits (degrees), its glide speed and the comfort-zone timer. */
    private static float viewYaw = Float.NaN;
    private static float viewElev;
    private static final float[] VEL = new float[2];
    private static boolean gliding;
    private static long outsideSince;
    /** VIEW: the text rests while you look within this many degrees of it ... */
    static final float COMFORT_DEG = 20f;
    /** ... and starts gliding after this long outside (at once when you turned far away). */
    static final long DELAY_NANOS = 350_000_000L;
    static final float FAR_DEG = 55f;
    /** Time the glide takes to get most of the way (critically damped: no overshoot) and its top speed. */
    static final float GLIDE_TIME = 0.5f;
    static final float GLIDE_MAX_SPEED = 170f;
    /** Height above where you look, and the band the text stays in (degrees above the horizon). */
    static final float VIEW_OFFSET = 9f;
    static final float VIEW_MIN = 9f;
    static final float VIEW_MAX = 58f;
    /** Frames in which world lyrics were handed to the renderer (the client test checks the hook works). */
    public static long drawnFrames;

    private WorldLyrics() {}

    public static void render(PoseStack pose, WorldSink sink) {
        if (!dev.henny.vibeyclient.module.media.lyrics.LyricsFeature.running()) return;
        SkyDome.render(pose, sink);
        LyricsConfig cfg = LyricsConfig.get();
        Minecraft mc = Minecraft.getInstance();
        if (cfg.position == LyricsConfig.Position.HUD || Playback.song() == null || mc.level == null || Playback.hidden()) return;
        Camera cam = LyricsCompat.camera();
        Vec3 eye = LyricsCompat.cameraPos(cam);
        float camYaw = LyricsCompat.cameraYaw(cam);
        float camPitch = LyricsCompat.cameraPitch(cam);
        // back after a pause (no song, other position, menu): start where you look instead of gliding there
        if (lastNanos != 0 && System.nanoTime() - lastNanos > 1_000_000_000L) resetView();
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
                    case VIEW -> view(camYaw, camPitch);
                    case FOLLOW -> smoothYaw;
                    case FIXED -> cfg.skyYaw;
                    default -> soft(camYaw);
                };
                // Looking down while you walk pulls the text lower so it stays in view; never below the horizon.
                float height = cfg.skyHeight;
                if (cfg.skyMode == LyricsConfig.SkyMode.VIEW) {
                    height = viewElev;
                } else if (cfg.skyMode != LyricsConfig.SkyMode.FIXED) {
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

    /**
     * "Where you look": the text rests in the sky while you look around near it; once your view has been outside a
     * comfort zone for a moment (or you turned far away) it glides – critically damped, so it eases in and never
     * overshoots – to where you look now, a little above your crosshair and never below the horizon or straight up.
     * Updated per frame with the real frame time, so it stays smooth at any frame rate and while turning quickly.
     */
    static float view(float camYaw, float camPitch) {
        float targetElev = Math.max(VIEW_MIN, Math.min(VIEW_MAX, -camPitch + VIEW_OFFSET));
        if (Float.isNaN(viewYaw)) {
            viewYaw = camYaw;
            viewElev = targetElev;
            VEL[0] = VEL[1] = 0f;
            gliding = false;
            return viewYaw;
        }
        float dYaw = wrap(camYaw - viewYaw);
        float dElev = targetElev - viewElev;
        float horiz = dYaw * (float) Math.cos(Math.toRadians(viewElev));
        float dist = (float) Math.sqrt(horiz * horiz + dElev * dElev);
        long now = System.nanoTime();
        if (!gliding) {
            if (dist > COMFORT_DEG) {
                if (outsideSince == 0) outsideSince = now;
                if (now - outsideSince > DELAY_NANOS || dist > FAR_DEG) gliding = true;
            } else {
                outsideSince = 0;
            }
        }
        if (gliding) {
            float y = smoothDamp(viewYaw, viewYaw + dYaw, 0, dt);
            float e = smoothDamp(viewElev, targetElev, 1, dt);
            viewYaw = wrap(y);
            viewElev = e;
            float speed = Math.abs(VEL[0]) + Math.abs(VEL[1]);
            if (dist < 1.2f && speed < 3f) {
                gliding = false;
                outsideSince = 0;
                VEL[0] = VEL[1] = 0f;
            }
        }
        return viewYaw;
    }

    /** Forget the VIEW position (new world, teleport): the next frame puts the text where you look. */
    public static void resetView() {
        viewYaw = Float.NaN;
        softYaw = Float.NaN;
        smoothYaw = Float.NaN;
    }

    /** Critically damped spring towards {@code target} (Game Programming Gems 4, 1.10), speed kept in VEL[slot]. */
    private static float smoothDamp(float current, float target, int slot, float dt) {
        float omega = 2f / GLIDE_TIME;
        float x = omega * dt;
        float exp = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x);
        float change = current - target;
        float maxChange = GLIDE_MAX_SPEED * GLIDE_TIME;
        change = Math.max(-maxChange, Math.min(maxChange, change));
        float t = current - change;
        float temp = (VEL[slot] + omega * change) * dt;
        VEL[slot] = (VEL[slot] - omega * temp) * exp;
        float out = t + (change + temp) * exp;
        if ((target - current > 0f) == (out > target)) {
            out = target;
            VEL[slot] = 0f;
        }
        return out;
    }

    /** VIEW state for tests: {yaw, elevation, gliding ? 1 : 0}. */
    public static float[] viewState() {
        return new float[]{viewYaw, viewElev, gliding ? 1f : 0f};
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

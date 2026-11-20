package dev.henny.vibeyclient.module.media.lyrics.world;

import dev.henny.r3.anchor.Facing;
import dev.henny.r3.object.CustomContent;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.WorldCanvas;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.render.LyricLayout;
import net.minecraft.client.Minecraft;
import org.joml.Vector3dc;

final class LyricsContent implements CustomContent {
    static final double IN_FRONT = 3.5;

    private CanvasPainter painter;
    private LyricsMode mode = LyricsMode.SKY;
    private float frameWidth = 1f;
    private float frameHeight = 1f;
    private long drawn;

    void mode(LyricsMode mode) {
        this.mode = mode;
    }

    long drawn() {
        return drawn;
    }

    float frameWidth() {
        return frameWidth;
    }

    float frameHeight() {
        return frameHeight;
    }

    @Override
    public Facing facing() {
        return mode == LyricsMode.WORLD && !LyricsConfig.get().anchorBillboard ? Facing.KEEP : Facing.CAMERA;
    }

    @Override
    public boolean translucent() {
        return true;
    }

    @Override
    public double boundingRadius(Transform transform) {
        return Math.max(4, transform.boundingRadius());
    }

    @Override
    public void draw(WorldCanvas canvas, WorldObject object, int light) {
        if (Playback.song() == null) return;
        Minecraft mc = Minecraft.getInstance();
        LyricsConfig cfg = LyricsConfig.get();
        Vector3dc cam = canvas.cameraPosition();
        Transform t = object.transform();
        double rx = t.position().x - cam.x(), ry = t.position().y - cam.y(), rz = t.position().z - cam.z();
        double dist = Math.sqrt(rx * rx + ry * ry + rz * rz);
        float scale = mode.scale(dist, cfg.size / 100f) * (mode == LyricsMode.WORLD ? t.scale() : 1f);
        LyricLayout.Frame frame = LyricLayout.build(mc.font, Playback.lyrics(), Playback.song().title(), Playback.song().artist(),
                Playback.lyricTimeMs(), mode.maxWidth(), (float) (dist / scale));
        if (frame.isEmpty()) return;
        frameWidth = Math.max(1f, frame.width() * scale / Math.max(0.01f, t.scale()));
        frameHeight = Math.max(1f, frame.height() * scale / Math.max(0.01f, t.scale()));
        canvas.push();
        if (mode == LyricsMode.SKY && cfg.skyOnTop && dist > IN_FRONT) {
            float k = (float) (IN_FRONT / dist);
            canvas.scale(k, k, k);
        }
        canvas.translate((float) rx, (float) ry, (float) rz);
        canvas.rotate(t.orientation());
        canvas.scale(scale, -scale, scale);
        if (painter == null) painter = new CanvasPainter(mc.font);
        painter.target(canvas, cfg.font, cfg.bold).paint(frame);
        canvas.pop();
        drawn++;
    }
}

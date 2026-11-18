package dev.henny.vibeyclient.module.media.video.player;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * A playing source. Created and used on the client thread; implementations do their decoding elsewhere.
 * {@link #frame(boolean)} is called once per rendered frame before anything is drawn.
 */
public interface MediaPlayer extends AutoCloseable {
    enum Status { LOADING, PLAYING, PAUSED, ENDED, ERROR }

    void play();

    void pause();

    void seek(long ms);

    long positionMs();

    /** -1 when unknown (live streams, web pages). */
    long durationMs();

    Status status();

    /** User-facing reason when {@link #status()} is ERROR. */
    String error();

    /** Texture to draw, null while nothing has been decoded yet. */
    Identifier texture();

    /** Texture size in pixels (the aspect the picture was letterboxed to). */
    int width();

    int height();

    /** Uploads the newest due frame; skip uploads when the picture is not visible. */
    void frame(boolean visible);

    /**
     * Sound: gain 0..1 (all volume settings and the distance already applied) and where it comes from; {@code pos}
     * null means "in your head" (overlay).
     */
    void audio(float gain, Vec3 pos);

    /** Only sound, nothing to show (e.g. an mp3). */
    default boolean audioOnly() {
        return false;
    }

    /** A web page that wants mouse/keyboard input ({@code BrowserScreen}). */
    default boolean interactive() {
        return false;
    }

    /** Whether the reported position can be trusted for drift correction. */
    default boolean seekable() {
        return durationMs() > 0;
    }

    @Override
    void close();
}

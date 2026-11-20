package dev.henny.vibeyclient.media.player;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public interface MediaPlayer extends AutoCloseable {
    enum Status { LOADING, PLAYING, PAUSED, ENDED, ERROR }

    void play();

    void pause();

    void seek(long ms);

    long positionMs();

    long durationMs();

    Status status();

    String error();

    Identifier texture();

    int width();

    int height();

    void frame(boolean visible);

    void audio(float gain, Vec3 pos);

    default boolean audioOnly() {
        return false;
    }

    default boolean interactive() {
        return false;
    }

    default boolean seekable() {
        return durationMs() > 0;
    }

    @Override
    void close();
}

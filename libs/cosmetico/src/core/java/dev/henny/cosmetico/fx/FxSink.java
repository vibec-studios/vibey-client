package dev.henny.cosmetico.fx;

import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Sound;

import java.util.UUID;

public interface FxSink {
    void particle(String id, double x, double y, double z, float vx, float vy, float vz, int count, float sx, float sy, float sz);

    void sound(String id, double x, double y, double z, float volume, float pitch);

    void embedded(Sound sound, double x, double y, double z, float volume, float pitch);

    void sprite(UUID owner, Bundle bundle, SpriteSpawn spawn);

    FxSink NONE = new FxSink() {
        @Override
        public void particle(String id, double x, double y, double z, float vx, float vy, float vz, int count, float sx, float sy, float sz) {}

        @Override
        public void sound(String id, double x, double y, double z, float volume, float pitch) {}

        @Override
        public void embedded(Sound sound, double x, double y, double z, float volume, float pitch) {}

        @Override
        public void sprite(UUID owner, Bundle bundle, SpriteSpawn spawn) {}
    };
}

package dev.henny.cosmetico.fixtures;

import dev.henny.cosmetico.fx.FxSink;
import dev.henny.cosmetico.fx.SpriteSpawn;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Sound;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RecordingFx implements FxSink {
    public final List<String> calls = new ArrayList<>();

    @Override
    public void particle(String id, double x, double y, double z, float vx, float vy, float vz, int count, float sx, float sy, float sz) {
        calls.add("particle " + id + " " + count + " " + x + " " + y + " " + z);
    }

    @Override
    public void sound(String id, double x, double y, double z, float volume, float pitch) {
        calls.add("sound " + id + " " + volume);
    }

    @Override
    public void embedded(Sound sound, double x, double y, double z, float volume, float pitch) {
        calls.add("embedded " + sound.id() + " " + volume);
    }

    @Override
    public void sprite(UUID owner, Bundle bundle, SpriteSpawn spawn) {
        calls.add("sprite " + spawn.sprite() + " " + spawn.count());
    }
}

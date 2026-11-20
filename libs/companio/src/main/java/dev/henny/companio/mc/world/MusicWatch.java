package dev.henny.companio.mc.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

final class MusicWatch {
    private static final double RANGE = 20;

    private final List<SoundInstance> records = new ArrayList<>();
    private SoundManager listening;

    boolean near(double x, double y, double z) {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        if (listening != sm) {
            sm.addListener(this::heard);
            listening = sm;
        }
        records.removeIf(s -> !sm.isActive(s));
        for (SoundInstance s : records) {
            double dx = s.getX() - x, dy = s.getY() - y, dz = s.getZ() - z;
            if (dx * dx + dy * dy + dz * dz < RANGE * RANGE) return true;
        }
        return false;
    }

    private void heard(SoundInstance s, WeighedSoundEvents events, float range) {
        if (s.getSource() == SoundSource.RECORDS && !s.isRelative() && records.size() < 16 && !records.contains(s)) records.add(s);
    }
}

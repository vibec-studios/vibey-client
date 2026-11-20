package dev.henny.vibeyclient.module.visuals.particles.core;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class ParticleStyles {
    private final Map<String, ParticleStyle> styles = new TreeMap<>();
    private int revision;

    public synchronized ParticleStyle get(String id) {
        ParticleStyle s = styles.get(id);
        return s == null ? ParticleStyle.VANILLA : s;
    }

    public synchronized void set(String id, ParticleStyle style) {
        if (id == null) return;
        if (style == null || style.vanilla()) styles.remove(id);
        else styles.put(id, style);
        revision++;
    }

    public synchronized void replaceAll(Map<String, ParticleStyle> next) {
        styles.clear();
        next.forEach((id, s) -> {
            if (id != null && s != null && !s.vanilla()) styles.put(id, s);
        });
        revision++;
    }

    public synchronized void clear() {
        styles.clear();
        revision++;
    }

    public synchronized Map<String, ParticleStyle> all() {
        return Collections.unmodifiableMap(new TreeMap<>(styles));
    }

    public synchronized int changed() {
        return styles.size();
    }

    public synchronized int revision() {
        return revision;
    }
}

package dev.henny.vibeyclient.module.visuals.particles.core;

import java.util.Locale;

public final class ParticleNames {
    private ParticleNames() {}

    public static String pretty(String id) {
        if (id == null || id.isEmpty()) return "";
        int colon = id.indexOf(':');
        String path = colon < 0 ? id : id.substring(colon + 1);
        String words = path.replace('_', ' ').replace('/', ' ').trim();
        if (words.isEmpty()) return id;
        String name = Character.toUpperCase(words.charAt(0)) + words.substring(1);
        boolean vanilla = colon < 0 || id.startsWith("minecraft:");
        return vanilla ? name : name + " (" + id.substring(0, colon) + ")";
    }

    public static boolean matches(String id, String name, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase(Locale.ROOT);
        return id.toLowerCase(Locale.ROOT).contains(q.replace(' ', '_'))
                || name.toLowerCase(Locale.ROOT).contains(q);
    }
}

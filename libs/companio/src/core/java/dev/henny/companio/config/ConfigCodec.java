package dev.henny.companio.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ConfigCodec {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigCodec() {}

    public static CompanioConfig read(String json) {
        if (json == null || json.isBlank()) return new CompanioConfig();
        try {
            CompanioConfig c = GSON.fromJson(json, CompanioConfig.class);
            if (c == null) return new CompanioConfig();
            if (c.saved == null) c.saved = new java.util.HashMap<>();
            if (c.selected == null) c.selected = "";
            if (c.name == null) c.name = "";
            if (c.variant == null) c.variant = "";
            if (c.command == null) c.command = "follow";
            c.volume = Math.max(0f, Math.min(1f, c.volume));
            return c;
        } catch (RuntimeException e) {
            return new CompanioConfig();
        }
    }

    public static String write(CompanioConfig c) {
        return GSON.toJson(c);
    }
}

package dev.henny.vibeyclient.module.social.model;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.backend.J;

public record Destination(String host, int port, String name) {
    public static Destination from(JsonObject o) {
        String host = J.str(o, "host");
        if (host == null || host.isBlank()) return null;
        int port = J.num(o, "port", 25565);
        return new Destination(host.trim(), port < 1 || port > 65535 ? 25565 : port, J.str(o, "name"));
    }
}

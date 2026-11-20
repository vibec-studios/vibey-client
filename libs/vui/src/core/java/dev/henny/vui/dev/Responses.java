package dev.henny.vui.dev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class Responses {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private Responses() {
    }

    static void json(HttpExchange ex, int status, JsonElement body) throws IOException {
        send(ex, status, "application/json; charset=utf-8", GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
    }

    static void error(HttpExchange ex, int status, String message) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("error", message);
        json(ex, status, o);
    }

    static void png(HttpExchange ex, byte[] bytes) throws IOException {
        send(ex, 200, "image/png", bytes);
    }

    static void send(HttpExchange ex, int status, String type, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}

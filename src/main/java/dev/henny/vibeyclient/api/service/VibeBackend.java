package dev.henny.vibeyclient.api.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The logged-in connection to the Vibe backend (REST + gateway), registered by the social module (which owns login,
 * tokens and the gateway). Other modules (cosmetics/emotes sync) use it without knowing about auth.
 * Look it up with {@code Services.get(VibeBackend.class)}; absent when online features or social are off.
 */
public interface VibeBackend {
    /** Base URL, e.g. {@code https://vibe.henny.dev}. */
    String baseUrl();

    /** Logged in and the gateway is ready (after {@code ready}/{@code resumed}). */
    boolean isReady();

    /** Our user id (= Minecraft UUID), or null when not logged in. */
    UUID selfId();

    /**
     * Authenticated REST call ({@code path} like {@code /api/v1/cosmetics/me}); handles token refresh. Completes
     * exceptionally with {@link ApiException} on error responses. Never call {@code join()} on the render thread.
     */
    CompletableFuture<JsonElement> request(String method, String path, JsonElement body);

    /** One part of a multipart request ({@code filename}/{@code contentType} null for plain fields). */
    record Part(String name, String filename, String contentType, byte[] bytes) {
        public static Part field(String name, String value) {
            return new Part(name, null, null, value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /** Authenticated multipart POST (uploads, e.g. cosmetic submissions). Same errors as {@link #request}. */
    default CompletableFuture<JsonElement> requestMultipart(String path, java.util.List<Part> parts) {
        return CompletableFuture.failedFuture(new ApiException(0, "unsupported", "multipart not supported"));
    }

    /** Sends a client op over the gateway ({@code {"op": op, "d": d}}); dropped when not connected. */
    void sendOp(String op, JsonObject d);

    /**
     * Listens to a gateway event type ({@code t}, e.g. {@code loadout.updated}); the handler runs on the client
     * thread. Close the returned handle to stop listening.
     */
    AutoCloseable onEvent(String type, Consumer<JsonObject> handler);

    /** Backend error with the protocol's error code ({@code rate_limited}, {@code forbidden} …). */
    final class ApiException extends RuntimeException {
        public final int status;
        public final String code;
        /** Seconds to wait before retrying ({@code rate_limited}), or -1. */
        public final int retryAfter;
        /** The error's {@code details} (e.g. validation problems), or null. */
        public final JsonElement details;

        public ApiException(int status, String code, String message) {
            this(status, code, message, -1, null);
        }

        public ApiException(int status, String code, String message, int retryAfter, JsonElement details) {
            super(message);
            this.status = status;
            this.code = code;
            this.retryAfter = retryAfter;
            this.details = details;
        }
    }

    /** Posted on the event bus when the backend became ready (also after a reconnect with a fresh {@code ready}). */
    record Ready(UUID selfId) {}
}

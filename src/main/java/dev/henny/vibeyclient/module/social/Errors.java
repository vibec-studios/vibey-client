package dev.henny.vibeyclient.module.social;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.service.VibeBackend.ApiException;
import dev.henny.vibeyclient.backend.BackendClient;

/**
 * User-facing texts for backend errors: own translations for known codes, the backend's (German) message for
 * everything else and where the message carries details (ban reason, validation).
 */
public final class Errors {
    private Errors() {}

    public static String text(Throwable t) {
        ApiException e = BackendClient.unwrap(t);
        return text(e.code, e.getMessage(), e.retryAfter);
    }

    public static String text(String code, String message, int retryAfter) {
        if (code == null) return message == null ? Lang.tr("social.error.generic") : message;
        switch (code) {
            case "banned", "validation_failed", "bad_request", "limit_reached", "muted" -> {
                if (message != null && !message.isBlank()) return message;
            }
            case "rate_limited" -> {
                return retryAfter > 0 ? Lang.tr("social.error.rate_limited_s", retryAfter) : Lang.tr("social.error.rate_limited");
            }
            default -> {
            }
        }
        String key = "social.error." + code;
        if (Lang.has(key)) return Lang.tr(key);
        if (message != null && !message.isBlank()) return message;
        return Lang.tr("social.error.generic");
    }
}

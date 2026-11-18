package dev.henny.vibeyclient.api.service;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Toasts and notifications from any module. The UI side (toast stack, notification center) registers the
 * {@link Sink}; without one, messages only go to the log. Safe to call from any thread.
 */
public final class Notify {
    private Notify() {}

    public enum Kind { INFO, SUCCESS, WARN, ERROR, SOCIAL }

    /**
     * @param title    short headline (already translated)
     * @param body     one or two lines, may be null
     * @param kind     colour/icon
     * @param iconKey  optional: a player UUID string for a head icon, or an icon id; may be null
     * @param action   optional click action (runs on the client thread); may be null
     */
    public record Toast(String title, String body, Kind kind, String iconKey, Runnable action) {}

    public interface Sink {
        void show(Toast toast);
    }

    private static final AtomicReference<Sink> SINK = new AtomicReference<>();

    public static void setSink(Sink sink) {
        SINK.set(sink);
    }

    public static void show(Toast toast) {
        Sink s = SINK.get();
        if (s != null) s.show(toast);
        else dev.henny.vibeyclient.Vibey.LOG.info("[toast] {}: {}", toast.title(), toast.body());
    }

    public static void info(String title, String body) {
        show(new Toast(title, body, Kind.INFO, null, null));
    }

    public static void success(String title, String body) {
        show(new Toast(title, body, Kind.SUCCESS, null, null));
    }

    public static void warn(String title, String body) {
        show(new Toast(title, body, Kind.WARN, null, null));
    }

    public static void error(String title, String body) {
        show(new Toast(title, body, Kind.ERROR, null, null));
    }
}
